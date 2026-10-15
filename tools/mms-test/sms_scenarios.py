#!/usr/bin/env python3
# SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
# SPDX-License-Identifier: GPL-3.0-or-later

"""
Runs SMS scenarios end to end on an EMULATOR, and checks what ended up in the telephony provider,
what the system was asked to send, and what the app logged.

  ./sms_scenarios.py [--adb ADB] [--serial emulator-5554] [NAME ...]

The emulator's modem really sends, receives and reports delivery of SMS, for free. Results the
modem can't produce (a part failing, a network error with a given cause, a delivery that failed)
are handed to the app's real receivers by the debug-only TestMmsPushReceiver.

This refuses to run on anything but an emulator: on a phone every scenario would send real SMS.
Needs the debug build installed and set as the default SMS app.
"""

import argparse
import re
import shlex
import subprocess
import sys
import time
import uuid

PACKAGE = "org.prauga.messages.debug"
RECEIVER = f"{PACKAGE}/org.prauga.messages.receiver.TestMmsPushReceiver"

TYPE_INBOX, TYPE_SENT, TYPE_OUTBOX, TYPE_FAILED = 1, 2, 4, 5
STATUS_COMPLETE, STATUS_PENDING, STATUS_FAILED = 0, 32, 64

RESULT_OK, RESULT_CANCELED = -1, 0
GENERIC_FAILURE, RADIO_OFF, NO_SERVICE, NETWORK_ERROR = 1, 2, 4, 17

LONG_TEXT = " ".join(f"word{i:03d}" for i in range(60))

class Device:
    def __init__(self, args):
        self.args = args

    def adb(self, *command, check=True):
        prefix = [self.args.adb, "-s", self.args.serial]
        result = subprocess.run([*prefix, *command], capture_output=True, text=True)
        if check and result.returncode != 0:
            raise RuntimeError(f"adb {' '.join(command)}: {result.stderr.strip()}")
        return result.stdout

    def shell(self, command, check=False):
        return self.adb("shell", command, check=check)

    def broadcast(self, action, *extras):
        command = ["am", "broadcast", "-a", f"org.prauga.messages.{action}", "-n", RECEIVER,
                   *extras]
        self.adb("shell", " ".join(shlex.quote(c) for c in command))

    def rows(self, limit=60):
        columns = ["_id", "thread_id", "address", "type", "status", "error_code", "read", "seen",
                   "protocol", "reply_path_present", "service_center", "date", "date_sent", "body"]
        command = ["am", "broadcast", "-a", "org.prauga.messages.TEST_SMS_DUMP", "-n", RECEIVER,
                   "--ei", "limit", str(limit)]
        output = self.adb("shell", " ".join(shlex.quote(c) for c in command))
        match = re.search(r'data="(.*)"\s*$', output, re.S)
        rows = []
        for line in (match.group(1).split("\n") if match and match.group(1) else []):
            values = line.split("\t")
            if len(values) == len(columns):
                rows.append(dict(zip(columns, values)))
        return rows

    def row(self, sms_id):
        return next((r for r in self.rows() if r["_id"] == sms_id), None)

    def log(self):
        return self.adb("logcat", "-d", "-b", "all", check=False)

    def sends(self):
        return len(re.findall(rf"SmsController: send\w+ caller={re.escape(PACKAGE)}", self.log()))

    def crashed(self):
        crash = self.adb("logcat", "-d", "-b", "crash", check=False)
        return "FATAL EXCEPTION" in crash and PACKAGE in crash

    def notifications(self):
        dump = self.adb("shell", "dumpsys", "notification", "--noredact", check=False)
        texts, current = [], False
        for line in dump.splitlines():
            if "NotificationRecord(" in line:
                current = f"pkg={PACKAGE}" in line
            elif current and ("android.text=" in line or "android.title=" in line):
                texts.append(line.strip())
        return texts

def wait_for(description, condition, timeout=60, interval=1):
    deadline = time.time() + timeout
    while time.time() < deadline:
        value = condition()
        if value:
            return value
        time.sleep(interval)
    raise AssertionError(f"timed out waiting for {description}")

def stays(description, condition, duration=8, interval=1):
    deadline = time.time() + duration
    while time.time() < deadline:
        assert condition(), description
        time.sleep(interval)

def semi_octets(digits):
    digits += "F" if len(digits) % 2 else ""
    return "".join(digits[i + 1] + digits[i] for i in range(0, len(digits), 2))

def sms_deliver(text, sender="5551234", pid=0x00, dcs=0x08, reply_path=False,
                service_center="15550009999"):
    """A GSM SMS-DELIVER as hex, its text in UCS-2"""
    sca = f"{len(semi_octets(service_center)) // 2 + 1:02X}91{semi_octets(service_center)}"
    first = 0x04 | (0x80 if reply_path else 0)
    body = text.encode("utf-16-be")
    return (f"{sca}{first:02X}{len(sender):02X}81{semi_octets(sender)}{pid:02X}{dcs:02X}"
            f"52011051000000{len(body):02X}{body.hex().upper()}")

def status_report(status, recipient="15550001001"):
    timestamp = "52011051000000"
    return ("00"
            "06"
            "01"
            f"{len(recipient):02X}91{semi_octets(recipient)}"
            f"{timestamp}{timestamp}{status:02X}")

class Context:
    def __init__(self, device):
        self.device = device

    def send(self, text=None, to="+15550001001", delay=None):
        text = text or f"sms {uuid.uuid4().hex[:8]}"
        before = {r["_id"] for r in self.device.rows()}
        extras = ["--es", "to", to, "--es", "text", text]
        if delay:
            extras += ["--ei", "delay", str(delay)]
        self.device.broadcast("TEST_MMS_SEND", *extras)
        row = wait_for("the message to be stored", lambda: next(
            (r for r in self.device.rows() if r["_id"] not in before and r["body"] == text), None),
                       timeout=20)
        return row["_id"]

    def receive_pdu(self, text, **pdu):
        self.device.broadcast("TEST_SMS_RECEIVE", "--allow-background-activity-starts",
                              "--es", "pdus", sms_deliver(text, **pdu))
        return wait_for("the SMS to be received", lambda: [
            r for r in self.device.rows() if r["body"] == text and r["type"] == "1"], timeout=30)

    def receive(self, text, sender="5551234"):
        before = {r["_id"] for r in self.device.rows()}
        self.device.adb("emu", "sms", "send", sender, text)
        return wait_for("the SMS to be received", lambda: [
            r for r in self.device.rows() if r["_id"] not in before and r["type"] == "1"],
                        timeout=30)

    def result(self, sms_id, result, error_code=-1, part=0, parts=1, attempt=1):
        self.device.broadcast("TEST_SMS_RESULT", "--es", "uri", f"content://sms/{sms_id}",
                              "--es", "kind", "sent", "--ei", "result", str(result),
                              "--ei", "errorCode", str(error_code), "--ei", "part", str(part),
                              "--ei", "parts", str(parts), "--el", "attempt", str(attempt))

    def delivered(self, sms_id, status):
        self.device.broadcast("TEST_SMS_RESULT", "--es", "uri", f"content://sms/{sms_id}",
                              "--es", "kind", "delivered", "--es", "pdu", status_report(status))

    def delayed(self, sms_id, op):
        self.device.broadcast("TEST_SMS_DELAYED", "--es", "uri", f"content://sms/{sms_id}",
                              "--es", "op", op)

    def notification_retry(self, sms_id):
        self.device.broadcast("TEST_SMS_NOTIFICATION_RETRY", "--es", "uri",
                              f"content://sms/{sms_id}")

    def delete(self, sms_id):
        self.device.broadcast("TEST_DELETE", "--es", "uri", f"content://sms/{sms_id}")

    def type_of(self, sms_id):
        row = self.device.row(sms_id)
        return row and row["type"]

    def waiting(self, delay=600_000):
        return self.send(delay=delay)

    def airplane(self, on):
        self.device.shell(f"cmd connectivity airplane-mode {'enable' if on else 'disable'}")

def receive_plain(ctx):
    text = f"plain {uuid.uuid4().hex[:6]}"
    rows = ctx.receive(text)
    assert len(rows) == 1 and rows[0]["body"] == text, rows
    wait_for("a notification", lambda: any(text in n for n in ctx.device.notifications()), 20)

def receive_long_multipart(ctx):
    rows = ctx.receive(LONG_TEXT)
    assert len(rows) == 1, f"expected one message, got {len(rows)}"
    assert rows[0]["body"] == LONG_TEXT, rows[0]["body"]

def receive_unicode(ctx):
    text = "नमस्ते héllo ✓ Привет"
    rows = ctx.receive(text)
    assert rows[0]["body"] == text, rows[0]["body"]

def receive_alphanumeric_sender(ctx):
    rows = ctx.receive("Your code is 123456", sender="VM-PRAUGA")
    assert rows[0]["address"], rows[0]

def receive_keeps_what_the_network_sent(ctx):
    text = f"details {uuid.uuid4().hex[:6]}"
    row = ctx.receive_pdu(text, pid=0x00, reply_path=True, service_center="15550009999")[0]
    assert row["body"] == text, row
    assert row["protocol"] == "0", row
    assert row["reply_path_present"] == "1", row
    assert row["service_center"].endswith("15550009999"), row
    assert row["date_sent"] and row["date_sent"] != "0", row

def receive_replace_takes_the_place_of_the_last(ctx):
    sender = "5557001"
    first = ctx.receive_pdu("balance 10", sender=sender, pid=0x41)[0]
    second = ctx.receive_pdu("balance 20", sender=sender, pid=0x41)[0]
    wait_for("the first one to be replaced",
             lambda: not any(r["body"] == "balance 10" for r in ctx.device.rows()), 10)
    assert ctx.device.row(second["_id"])["body"] == "balance 20"
    third = ctx.receive_pdu("hello", sender=sender, pid=0x00)[0]
    stays("a normal SMS replaces nothing", lambda: ctx.device.row(second["_id"]), 3)
    assert ctx.device.row(third["_id"])

def _class_zero(ctx, button):
    text = f"flash {uuid.uuid4().hex[:6]}"
    row = ctx.receive_pdu(text, sender="5557002", dcs=0x18)[0]
    shown = wait_for("the flash SMS to pop up", lambda: "ClassZeroActivity" in ctx.device.shell(
        "dumpsys activity activities | grep -E 'topResumedActivity|ResumedActivity'"), 15)
    assert shown
    assert not any(text in n for n in ctx.device.notifications()), "it shouldn't be notified"
    dump = ctx.device.shell("uiautomator dump /sdcard/ui.xml >/dev/null; cat /sdcard/ui.xml")
    match = re.search(rf'text="{button}"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', dump,
                      re.I)
    assert match, f"no {button} button"
    x1, y1, x2, y2 = map(int, match.groups())
    ctx.device.shell(f"input tap {(x1 + x2) // 2} {(y1 + y2) // 2}")
    return row, text

def receive_class_zero_saved(ctx):
    row, text = _class_zero(ctx, "Save")
    wait_for("it to be kept as read", lambda: (ctx.device.row(row["_id"]) or {}).get("read") == "1")
    stays("and never notified", lambda: not any(text in n for n in ctx.device.notifications()), 3)

def receive_class_zero_dismissed(ctx):
    row, _ = _class_zero(ctx, "Cancel")
    wait_for("it to be dropped", lambda: ctx.device.row(row["_id"]) is None, 15)

def send_short(ctx):
    sends = ctx.device.sends()
    sms_id = ctx.send()
    wait_for("it to be sent", lambda: ctx.type_of(sms_id) == "2")
    assert ctx.device.sends() == sends + 1

def send_long_multipart(ctx):
    sms_id = ctx.send(LONG_TEXT)
    wait_for("it to be sent", lambda: ctx.type_of(sms_id) == "2")
    assert ctx.device.row(sms_id)["body"] == LONG_TEXT
    assert "sendMultipartTextForSubscriber" in ctx.device.log()

def send_delivery_report_from_the_network(ctx):
    sms_id = ctx.send(LONG_TEXT)
    wait_for("it to be delivered", lambda: ctx.device.row(sms_id)["status"] == str(STATUS_COMPLETE))

def part_failure_fails_the_whole_message(ctx):
    sms_id = ctx.waiting()
    ctx.result(sms_id, RESULT_OK, part=0, parts=3)
    ctx.result(sms_id, GENERIC_FAILURE, part=1, parts=3)
    ctx.result(sms_id, RESULT_OK, part=2, parts=3)
    wait_for("it to fail", lambda: ctx.type_of(sms_id) == "5")
    stays("a later part must not mark it sent", lambda: ctx.type_of(sms_id) == "5", 4)
    ctx.delete(sms_id)

def last_part_reporting_first_waits_for_the_rest(ctx):
    sms_id = ctx.waiting()
    ctx.result(sms_id, RESULT_OK, part=1, parts=2)
    stays("it waits for the other part", lambda: ctx.type_of(sms_id) == "4", 3)
    ctx.result(sms_id, GENERIC_FAILURE, part=0, parts=2)
    wait_for("it to fail", lambda: ctx.type_of(sms_id) == "5")
    ctx.delete(sms_id)

def worst_part_decides(ctx):
    sms_id = ctx.waiting()
    ctx.result(sms_id, NO_SERVICE, part=0, parts=3)
    ctx.result(sms_id, GENERIC_FAILURE, part=1, parts=3)
    ctx.result(sms_id, RESULT_OK, part=2, parts=3)
    wait_for("it to fail", lambda: ctx.type_of(sms_id) == "5")
    ctx.delete(sms_id)

def only_a_temporary_part_failure_is_retried(ctx):
    sms_id = ctx.waiting()
    ctx.result(sms_id, RESULT_OK, part=0, parts=2)
    ctx.result(sms_id, NO_SERVICE, part=1, parts=2)
    stays("it keeps showing as sending", lambda: ctx.type_of(sms_id) == "4", 2)
    wait_for("the retry to send it", lambda: ctx.type_of(sms_id) == "2", 60)

def all_parts_sent_marks_sent_once(ctx):
    sms_id = ctx.waiting()
    for part in range(3):
        if part < 2:
            ctx.result(sms_id, RESULT_OK, part=part, parts=3)
            stays("it isn't sent before its last part", lambda: ctx.type_of(sms_id) == "4", 2)
    ctx.result(sms_id, RESULT_OK, part=2, parts=3)
    wait_for("it to be sent", lambda: ctx.type_of(sms_id) == "2")
    ctx.delete(sms_id)

def every_part_failing_notifies_once(ctx):
    sms_id = ctx.waiting()
    for part in range(3):
        ctx.result(sms_id, GENERIC_FAILURE, part=part, parts=3)
    wait_for("it to fail", lambda: ctx.type_of(sms_id) == "5")
    wait_for("a failure notification", lambda: ctx.device.notifications(), 10)
    ctx.delete(sms_id)

def result_from_before_the_update_still_works(ctx):
    sms_id = ctx.waiting()
    ctx.device.broadcast("TEST_SMS_RESULT", "--es", "uri", f"content://sms/{sms_id}", "--es",
                         "kind", "sent", "--ei", "result", str(RESULT_OK), "--ei", "parts", "1")
    wait_for("it to be sent", lambda: ctx.type_of(sms_id) == "2")
    ctx.delete(sms_id)

def delivery_report_statuses(ctx):
    sms_id = ctx.send()
    wait_for("it to be sent", lambda: ctx.type_of(sms_id) == "2")
    wait_for("the network's report", lambda: ctx.device.row(sms_id)["status"] == "0")

    ctx.delivered(sms_id, 0x40)
    wait_for("it to show as not delivered",
             lambda: ctx.device.row(sms_id)["status"] == str(STATUS_FAILED))

    for status in (0x20, 0x46, 0x02):
        ctx.delivered(sms_id, status)
    stays("other reports change nothing",
          lambda: ctx.device.row(sms_id)["status"] == str(STATUS_FAILED), 4)

    ctx.device.broadcast("TEST_SMS_RESULT", "--es", "uri", f"content://sms/{sms_id}",
                         "--es", "kind", "delivered")
    stays("an empty report changes nothing",
          lambda: ctx.device.row(sms_id)["status"] == str(STATUS_FAILED), 3)

    ctx.delivered(sms_id, 0x00)
    wait_for("it to show as delivered",
             lambda: ctx.device.row(sms_id)["status"] == str(STATUS_COMPLETE))

def no_service_is_retried_by_itself(ctx):
    sms_id = ctx.waiting()
    sends = ctx.device.sends()
    ctx.result(sms_id, NO_SERVICE)
    stays("it keeps showing as sending", lambda: ctx.type_of(sms_id) == "4", 3)
    wait_for("the retry to send it", lambda: ctx.type_of(sms_id) == "2", 60)
    assert ctx.device.sends() == sends + 1, "expected exactly one retry"

def network_error_with_temporary_cause_is_retried(ctx):
    sms_id = ctx.waiting()
    ctx.result(sms_id, NETWORK_ERROR, error_code=41)
    wait_for("the retry to send it", lambda: ctx.type_of(sms_id) == "2", 60)

def network_error_with_other_cause_fails(ctx):
    sms_id = ctx.waiting()
    sends = ctx.device.sends()
    ctx.result(sms_id, NETWORK_ERROR, error_code=38)
    wait_for("it to fail", lambda: ctx.type_of(sms_id) == "5")
    stays("it isn't retried", lambda: ctx.device.sends() == sends, 10)
    ctx.delete(sms_id)

def generic_failure_fails_and_notifies(ctx):
    sms_id = ctx.waiting()
    ctx.result(sms_id, GENERIC_FAILURE)
    wait_for("it to fail", lambda: ctx.type_of(sms_id) == "5")
    wait_for("a failure notification", lambda: ctx.device.notifications(), 10)
    ctx.delete(sms_id)

def airplane_mode_then_back(ctx):
    ctx.airplane(True)
    try:
        time.sleep(3)
        sms_id = ctx.send()
        stays("it keeps showing as sending", lambda: ctx.type_of(sms_id) == "4", 10)
        assert re.search(r"SMS \d+ failed with .*retrying", ctx.device.log()), \
            "it wasn't scheduled for a retry"
    finally:
        ctx.airplane(False)
    wait_for("it to go once the radio is back", lambda: ctx.type_of(sms_id) == "2", 180, 3)

def service_back_sends_right_away(ctx):
    ctx.airplane(True)
    try:
        time.sleep(3)
        sms_id = ctx.send()
        time.sleep(80)
        assert ctx.type_of(sms_id) == "4", "it should still be waiting"
    finally:
        ctx.airplane(False)
    start = time.time()
    wait_for("it to go once the phone has service", lambda: ctx.type_of(sms_id) == "2", 60, 1)
    assert time.time() - start < 40, f"took {time.time() - start:.0f}s, not right away"

def delayed_send_goes_at_its_time(ctx):
    sms_id = ctx.send(delay=5000)
    assert ctx.type_of(sms_id) == "4"
    wait_for("it to be sent", lambda: ctx.type_of(sms_id) == "2", 40)

def delayed_cancel(ctx):
    sends = ctx.device.sends()
    sms_id = ctx.send(delay=30_000)
    ctx.delayed(sms_id, "cancel")
    wait_for("it to be deleted", lambda: ctx.device.row(sms_id) is None, 15)
    stays("it is never sent", lambda: ctx.device.sends() == sends, 5)

def delayed_send_now_sends_the_same_message_once(ctx):
    sends = ctx.device.sends()
    text = f"now {uuid.uuid4().hex[:6]}"
    sms_id = ctx.send(text, delay=30_000)
    ctx.delayed(sms_id, "now")
    wait_for("it to be sent", lambda: ctx.type_of(sms_id) == "2", 20)
    copies = [r for r in ctx.device.rows() if text in r["body"]]
    assert len(copies) == 1, f"send now made another message: {copies}"
    assert ctx.device.row(sms_id)["body"] == text, "the text changed"
    stays("the alarm doesn't send it again", lambda: ctx.device.sends() == sends + 1, 5)

def cancel_after_it_went_keeps_it(ctx):
    sms_id = ctx.send(delay=3000)
    wait_for("it to be sent", lambda: ctx.type_of(sms_id) == "2", 30)
    ctx.delayed(sms_id, "cancel")
    stays("it is kept", lambda: ctx.type_of(sms_id) == "2", 5)

def send_now_after_it_went_sends_nothing(ctx):
    sms_id = ctx.send(delay=3000)
    wait_for("it to be sent", lambda: ctx.type_of(sms_id) == "2", 30)
    sends = ctx.device.sends()
    ctx.delayed(sms_id, "now")
    stays("it isn't sent again", lambda: ctx.device.sends() == sends, 6)

def notification_retry_sends_a_failed_message_once(ctx):
    sms_id = ctx.waiting()
    ctx.result(sms_id, GENERIC_FAILURE)
    wait_for("it to fail", lambda: ctx.type_of(sms_id) == "5")
    sends = ctx.device.sends()
    ctx.notification_retry(sms_id)
    ctx.notification_retry(sms_id)
    wait_for("it to be sent", lambda: ctx.type_of(sms_id) == "2", 30)
    stays("the second tap sends nothing", lambda: ctx.device.sends() == sends + 1, 5)

def notification_retry_on_a_sent_message_does_nothing(ctx):
    sms_id = ctx.send()
    wait_for("it to be sent", lambda: ctx.type_of(sms_id) == "2")
    sends = ctx.device.sends()
    ctx.notification_retry(sms_id)
    stays("nothing is sent", lambda: ctx.device.sends() == sends, 6)

def reboot_resumes_unsent(ctx):
    passed = ctx.send(delay=15_000)
    later = ctx.send(delay=240_000)
    ctx.device.adb("reboot")
    time.sleep(10)
    ctx.device.adb("wait-for-device")
    wait_for("the emulator to boot",
             lambda: ctx.device.shell("getprop sys.boot_completed").strip() == "1", 300, 3)
    wait_for("the passed one to go after the boot", lambda: ctx.type_of(passed) == "2", 180, 3)
    assert ctx.type_of(later) == "4", "the later one should still wait"
    wait_for("the later one to go at its time", lambda: ctx.type_of(later) == "2", 300, 5)

SCENARIOS = [
    receive_plain, receive_long_multipart, receive_unicode, receive_alphanumeric_sender,
    send_short, send_long_multipart, send_delivery_report_from_the_network,
    receive_keeps_what_the_network_sent, receive_replace_takes_the_place_of_the_last,
    receive_class_zero_saved, receive_class_zero_dismissed,
    part_failure_fails_the_whole_message, last_part_reporting_first_waits_for_the_rest,
    worst_part_decides, only_a_temporary_part_failure_is_retried,
    all_parts_sent_marks_sent_once, every_part_failing_notifies_once,
    result_from_before_the_update_still_works,
    delivery_report_statuses,
    no_service_is_retried_by_itself, network_error_with_temporary_cause_is_retried,
    network_error_with_other_cause_fails, generic_failure_fails_and_notifies,
    airplane_mode_then_back, service_back_sends_right_away,
    delayed_send_goes_at_its_time, delayed_cancel, delayed_send_now_sends_the_same_message_once,
    cancel_after_it_went_keeps_it, send_now_after_it_went_sends_nothing,
    notification_retry_sends_a_failed_message_once,
    notification_retry_on_a_sent_message_does_nothing,
    reboot_resumes_unsent,
]

def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawTextHelpFormatter)
    parser.add_argument("--adb", default="adb")
    parser.add_argument("--serial", default="emulator-5554")
    parser.add_argument("names", nargs="*", help="only run these scenarios")
    args = parser.parse_args()

    if not args.serial.startswith("emulator-"):
        sys.exit("SMS scenarios really send SMS: only run them on an emulator")

    device = Device(args)
    ctx = Context(device)
    selected = [s for s in SCENARIOS if not args.names or s.__name__ in args.names]
    results = []
    for scenario in selected:
        device.adb("logcat", "-b", "all", "-c", check=False)
        start = time.time()
        try:
            scenario(ctx)
            assert not device.crashed(), "the app crashed"
            results.append((scenario.__name__, "PASS", ""))
        except Exception as e:
            results.append((scenario.__name__, "FAIL", str(e)))
        print(f"{results[-1][1]}  {scenario.__name__} ({time.time() - start:.0f}s) "
              f"{results[-1][2]}", flush=True)
        if results[-1][1] == "FAIL":
            crash = device.adb("logcat", "-d", "-b", "crash", check=False).strip()
            if PACKAGE in crash:
                print(crash[-2000:])

    failed = [r for r in results if r[1] == "FAIL"]
    print(f"\n{len(results) - len(failed)}/{len(results)} scenarios passed")
    sys.exit(1 if failed else 0)

if __name__ == "__main__":
    main()
