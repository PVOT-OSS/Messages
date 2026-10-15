#!/usr/bin/env python3
"""
Runs MMS scenarios end to end against the fake MMSC and checks what happened, on a device or
emulator prepared with ./setup.sh.

  ./scenarios.py [--serial SERIAL] [--host 127.0.0.1] [NAME ...]

Every scenario drives the real app and Android's real MmsService, then checks three things: what
the MMSC received, what the app logged, and what ended up in the telephony provider.
"""

import argparse
import json
import re
import subprocess
import sys
import time
import uuid

import mmsc as tool

PACKAGE = "org.prauga.messages.debug"

PERMANENT_FAILURE = 0x87
M_TYPE_NOTIFICATION = 130
M_TYPE_RETRIEVE_CONF = 132
BOX_INBOX, BOX_SENT, BOX_OUTBOX, BOX_FAILED = 1, 2, 4, 5

class Device:
    def __init__(self, args):
        self.args = args

    def adb(self, *command, check=True):
        result = subprocess.run([*tool.adb(self.args), *command], capture_output=True, text=True)
        if check and result.returncode != 0:
            raise RuntimeError(f"adb {' '.join(command)}: {result.stderr.strip()}")
        return result.stdout

    def logcat(self):
        return self.adb("logcat", "-d", "-b", "all")

    def clear_logcat(self):
        self.adb("logcat", "-b", "all", "-c")

    def mms_rows(self, where=None):
        columns = ["_id", "m_type", "msg_box", "ct_l", "st", "sub_id", "m_id", "resp_st", "date"]
        query = f"select {','.join(columns)} from pdu"
        if where:
            query += f" where {where}"
        output = self.adb("shell", f'sqlite3 -separator "|" {self.args.database} "{query};"',
                          check=False)
        rows = []
        for line in output.splitlines():
            values = line.split("|")
            if len(values) == len(columns):
                rows.append({c: (v if v != "" else None) for c, v in zip(columns, values)})
        return rows

def wait_for(description, condition, timeout=90, interval=1):
    deadline = time.time() + timeout
    while time.time() < deadline:
        value = condition()
        if value:
            return value
        time.sleep(interval)
    raise AssertionError(f"timed out waiting for {description}")

def assert_not_retried(ctx, kind, decided, wait=12):
    wait_for("the app to give up", lambda: ctx.app_log(decided))
    count = len(ctx.events(kind))
    time.sleep(wait)
    assert len(ctx.events(kind)) == count, f"the app tried again after {decided!r}"

def mmsc_log():
    return tool.load_state()["log"]

def mmsc_events(kind, since):
    return [entry for entry in mmsc_log()[since:] if entry["event"] == kind]

def set_scenario(name, value):
    state = tool.load_state()
    if name == "delay":
        state["scenario"]["delay"] = int(value)
    else:
        state["scenario"][name] = value
    tool.save_state(state)

def reset_scenario():
    state = tool.load_state()
    state["scenario"] = dict(tool.DEFAULT_SCENARIO)
    tool.save_state(state)

class Context:
    def __init__(self, device, args):
        self.device = device
        self.args = args
        self.log_start = len(mmsc_log())

    def push(self, message_id=None, **overrides):
        message_id = message_id or uuid.uuid4().hex[:8]
        push_args = argparse.Namespace(
            id=message_id, transaction=None, sender=overrides.get("sender", "+15551230000"),
            to="+15555550100", text=overrides.get("text", f"scenario {message_id}"),
            image=overrides.get("image", False), subscription=None, port=self.args.port,
            host=self.args.host, adb=self.args.adb, serial=self.args.serial, package=PACKAGE)
        tool.push(push_args)
        return message_id

    def send(self, *to, image=False, text=None):
        send_args = argparse.Namespace(
            to=list(to), text=text or f"send {uuid.uuid4().hex[:6]}", image=image,
            subscription=None, adb=self.args.adb, serial=self.args.serial, package=PACKAGE)
        tool.send(send_args)

    def broadcast(self, action, *extras):
        self.device.adb("shell", "am", "broadcast", "-a", f"org.prauga.messages.{action}",
                        "-n", f"{PACKAGE}/org.prauga.messages.receiver.TestMmsPushReceiver", *extras)

    def set_pref(self, key, value=None):
        extras = ["--es", "key", key]
        if value is not None:
            extras += ["--ez", "value", str(value).lower()]
        self.broadcast("TEST_SET_PREF", *extras)

    def download(self, row):
        self.broadcast("TEST_MMS_DOWNLOAD", "--es", "uri", f"content://mms/{row['_id']}")

    def open_app(self):
        self.device.adb("shell", "am", "start", "-W", "-n",
                        f"{PACKAGE}/org.prauga.messages.feature.main.MainActivity")

    def events(self, kind):
        return mmsc_events(kind, self.log_start)

    def location(self, message_id):
        return f"http://{self.args.host}:{self.args.port}/mms/{message_id}"

    def row_for(self, message_id):
        rows = self.device.mms_rows(f"ct_l='{self.location(message_id)}'")
        return rows[0] if rows else None

    def app_log(self, pattern):
        return re.findall(pattern, self.device.logcat())

    def latest_outgoing(self):
        rows = [row for row in self.device.mms_rows("m_type=128") if row.get("_id")]
        return max(rows, key=lambda row: int(row["_id"])) if rows else None

def receive_ok(ctx):
    message_id = ctx.push(image=True)
    row = wait_for("the downloaded message",
                   lambda: (r := ctx.row_for(message_id)) and r["m_type"] == str(M_TYPE_RETRIEVE_CONF) and r)
    assert row["msg_box"] == str(BOX_INBOX), row
    assert row["sub_id"] not in (None, "-1"), f"no SIM stored: {row}"
    responses = wait_for("a NotifyResp", lambda: ctx.events("notify-resp"))
    assert responses[-1]["status"] == hex(0x81), f"expected RETRIEVED, got {responses}"
    assert len(ctx.events("download")) == 1

def receive_duplicate_push(ctx):
    message_id = ctx.push()
    wait_for("the first download", lambda: (r := ctx.row_for(message_id)) and r["m_type"] == "132")
    ctx.push(message_id)
    time.sleep(6)
    assert len(ctx.events("download")) == 1, "a repeated push was downloaded again"
    assert ctx.app_log(r"Skip downloading duplicate message"), "duplicate wasn't recognised"
    assert len(ctx.device.mms_rows(f"ct_l='{ctx.location(message_id)}'")) == 1

def receive_404_keeps_expired_message(ctx):
    set_scenario("download", "404")
    message_id = ctx.push()
    row = wait_for("the expired notification",
                   lambda: (r := ctx.row_for(message_id)) and r["st"] == str(PERMANENT_FAILURE) and r)
    assert row["m_type"] == str(M_TYPE_NOTIFICATION), row
    assert_not_retried(ctx, "download", r"MMS download result: NEVER")

def _receive_retried(ctx, mode):
    set_scenario("download", mode)
    message_id = ctx.push()
    wait_for("the first attempt", lambda: ctx.events("download"))
    wait_for("the failure to be handled", lambda: ctx.app_log(r"MMS download result: AUTO"))
    row = ctx.row_for(message_id)
    assert row and row["m_type"] == str(M_TYPE_NOTIFICATION), f"notification should stay: {row}"
    set_scenario("download", "ok")
    row = wait_for("the automatic retry to download it",
                   lambda: (r := ctx.row_for(message_id)) and r["m_type"] == "132" and r, timeout=150)
    assert len(ctx.events("download")) >= 2, "downloaded without retrying"

def receive_server_error_retries(ctx):
    _receive_retried(ctx, "500")

def receive_bad_request_retries(ctx):
    _receive_retried(ctx, "400")

def receive_transient_status_retries(ctx):
    _receive_retried(ctx, "transient")

def receive_empty_response_retries(ctx):
    _receive_retried(ctx, "empty")

def receive_expired_status_is_permanent(ctx):
    set_scenario("download", "expired")
    message_id = ctx.push()
    wait_for("the expired notification",
             lambda: (r := ctx.row_for(message_id)) and r["st"] == str(PERMANENT_FAILURE))
    assert ctx.app_log(r"MMS download result: NEVER")

def receive_garbage_is_permanent(ctx):
    set_scenario("download", "garbage")
    message_id = ctx.push()
    wait_for("the failed notification",
             lambda: (r := ctx.row_for(message_id)) and r["st"] == str(PERMANENT_FAILURE))
    assert_not_retried(ctx, "download", r"MMS download result: NEVER")

def receive_auto_download_off_then_manual(ctx):
    ctx.set_pref("mmsAutoDownload", False)
    try:
        message_id = ctx.push()
        row = wait_for("the waiting notification", lambda: ctx.row_for(message_id))
        assert row["m_type"] == str(M_TYPE_NOTIFICATION), row
        responses = wait_for("a deferred NotifyResp", lambda: ctx.events("notify-resp"))
        assert responses[-1]["status"] == hex(0x83), f"expected DEFERRED, got {responses}"
        assert not ctx.events("download"), "downloaded although auto-download is off"

        ctx.download(row)
        wait_for("the manual download",
                 lambda: (r := ctx.row_for(message_id)) and r["m_type"] == "132")
        wait_for("an M-Acknowledge.ind", lambda: ctx.events("acknowledge"))
    finally:
        ctx.set_pref("mmsAutoDownload")

def receive_pending_downloaded_when_app_opens(ctx):
    ctx.set_pref("mmsAutoDownload", False)
    message_id = ctx.push()
    wait_for("the waiting notification", lambda: ctx.row_for(message_id))
    wait_for("the app to sync it", lambda: ctx.app_log(r"Not downloading MMS automatically"))
    time.sleep(2)

    ctx.set_pref("mmsAutoDownload")
    ctx.device.adb("shell", "am", "force-stop", PACKAGE)
    ctx.open_app()
    wait_for("the pending MMS to be downloaded",
             lambda: (r := ctx.row_for(message_id)) and r["m_type"] == "132", timeout=60)
    responses = ctx.events("notify-resp")
    assert responses[-1]["status"] == hex(0x81), f"expected RETRIEVED, got {responses}"

def send_ok(ctx):
    ctx.send("+15550000001", "+15550000002")
    row = wait_for("the message to be sent",
                   lambda: (r := ctx.latest_outgoing()) and r["msg_box"] == str(BOX_SENT) and r)
    assert row["m_id"], f"the carrier's message id wasn't stored: {row}"
    assert row["resp_st"] == "128", row
    assert len(ctx.events("send")) == 1

def send_with_image(ctx):
    ctx.send("+15550000003", image=True)
    wait_for("the message to be sent",
             lambda: (r := ctx.latest_outgoing()) and r["msg_box"] == str(BOX_SENT))
    sends = ctx.events("send")
    assert len(sends) == 1 and sends[0]["bytes"] > 300, sends

def _send_retried(ctx, mode):
    set_scenario("send", mode)
    ctx.send("+15550000004", "+15550000005")
    wait_for("the first attempt", lambda: ctx.events("send"))
    wait_for("the retry to be scheduled", lambda: ctx.app_log(r"MMS send failed, retrying later"))
    row = ctx.latest_outgoing()
    assert row["msg_box"] != str(BOX_FAILED), f"shown as failed while retrying: {row}"
    set_scenario("send", "ok")
    wait_for("the retry to send it",
             lambda: (r := ctx.latest_outgoing()) and r["msg_box"] == str(BOX_SENT), timeout=150)
    assert len(ctx.events("send")) >= 2, "sent without retrying"

def send_transient_failure_retries(ctx):
    _send_retried(ctx, "transient")

def send_server_error_retries(ctx):
    _send_retried(ctx, "500")

def _send_fails(ctx, mode):
    set_scenario("send", mode)
    ctx.send("+15550000006", "+15550000007")
    wait_for("the message to fail",
             lambda: (r := ctx.latest_outgoing()) and r["msg_box"] == str(BOX_FAILED))
    assert_not_retried(ctx, "send", r"MMS has failed to send")

def send_rejected_by_carrier(ctx):
    _send_fails(ctx, "permanent")

def send_service_denied(ctx):
    _send_fails(ctx, "denied")

def send_404(ctx):
    _send_fails(ctx, "404")

SCENARIOS = [
    receive_ok,
    receive_duplicate_push,
    receive_404_keeps_expired_message,
    receive_server_error_retries,
    receive_bad_request_retries,
    receive_transient_status_retries,
    receive_empty_response_retries,
    receive_expired_status_is_permanent,
    receive_garbage_is_permanent,
    receive_auto_download_off_then_manual,
    receive_pending_downloaded_when_app_opens,
    send_ok,
    send_with_image,
    send_transient_failure_retries,
    send_server_error_retries,
    send_rejected_by_carrier,
    send_service_denied,
    send_404,
]

def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawTextHelpFormatter)
    parser.add_argument("--adb", default="adb")
    parser.add_argument("--serial", default="emulator-5554")
    parser.add_argument("--host", default=tool.HOST_FROM_EMULATOR)
    parser.add_argument("--port", type=int, default=tool.PORT)
    parser.add_argument("--database",
                        default="/data/data/com.android.providers.telephony/databases/mmssms.db",
                        help="telephony provider database on the device")
    parser.add_argument("names", nargs="*", help="only run these scenarios")
    args = parser.parse_args()

    device = Device(args)
    selected = [s for s in SCENARIOS if not args.names or s.__name__ in args.names]
    results = []
    for scenario in selected:
        reset_scenario()
        device.clear_logcat()
        ctx = Context(device, args)
        start = time.time()
        try:
            scenario(ctx)
            results.append((scenario.__name__, "PASS", ""))
        except Exception as e:
            results.append((scenario.__name__, "FAIL", str(e)))
        print(f"{results[-1][1]}  {scenario.__name__} ({time.time() - start:.0f}s) {results[-1][2]}",
              flush=True)
        if results[-1][1] == "FAIL":
            crash = device.adb("logcat", "-d", "-b", "crash", check=False).strip()
            if crash:
                print(crash[-2000:])
    reset_scenario()

    failed = [r for r in results if r[1] == "FAIL"]
    print(f"\n{len(results) - len(failed)}/{len(results)} scenarios passed")
    sys.exit(1 if failed else 0)

if __name__ == "__main__":
    main()
