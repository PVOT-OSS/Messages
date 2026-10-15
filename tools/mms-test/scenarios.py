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

def sql(device, query, columns):
    output = device.adb("shell", f'sqlite3 -separator "|" {device.args.database} "{query};"',
                        check=False)
    rows = []
    for line in output.splitlines():
        values = line.split("|")
        if len(values) == len(columns):
            rows.append({c: (v if v != "" else None) for c, v in zip(columns, values)})
    return rows

def parts_of(device, mms_id):
    return sql(device, f"select ct,name,length(text),_data from part where mid={mms_id}",
               ["ct", "name", "text_length", "data"])

def addresses_of(device, mms_id):
    return sql(device, f"select address,type from addr where msg_id={mms_id}", ["address", "type"])

def app_crashed(device):
    return "FATAL EXCEPTION" in device.adb("logcat", "-d", "-b", "crash", check=False) and \
        PACKAGE in device.adb("logcat", "-d", "-b", "crash", check=False)

def notification_texts(device):
    dump = device.adb("shell", "dumpsys", "notification", "--noredact", check=False)
    texts, current = [], False
    for line in dump.splitlines():
        if "NotificationRecord(" in line:
            current = f"pkg={PACKAGE}" in line
        elif current and ("android.text=" in line or "android.title=" in line):
            texts.append(line.strip())
    return texts

def ui_dump(device):
    device.adb("shell", "uiautomator", "dump", "/sdcard/ui.xml", check=False)
    return device.adb("shell", "cat", "/sdcard/ui.xml", check=False)

def tap_view(device, resource_id):
    match = re.search(rf'resource-id="{PACKAGE}:id/{resource_id}"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"',
                      ui_dump(device))
    if not match:
        return False
    x1, y1, x2, y2 = map(int, match.groups())
    device.adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
    return True

def resumed_activity(device):
    dump = device.adb("shell", "dumpsys", "activity", "activities", check=False)
    match = re.search(r"topResumedActivity=.*? (\S+/\S+)", dump)
    return match.group(1) if match else ""

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
    with tool.lock:
        state = tool.load_state()
        if name == "delay":
            state["scenario"]["delay"] = int(value)
        else:
            state["scenario"][name] = value
        tool.save_state(state)

def reset_scenario():
    with tool.lock:
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
            to=overrides.get("to", ["+15555550100"]), cc=overrides.get("cc", []),
            text=overrides.get("text", f"scenario {message_id}"),
            no_text=overrides.get("no_text", False), subject=overrides.get("subject"),
            parts=overrides.get("parts"),
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

    def block(self, address, unblock=False):
        extras = ["--es", "address", address]
        if unblock:
            extras += ["--ez", "unblock", "true"]
        self.broadcast("TEST_BLOCK", *extras)
        time.sleep(4)

    def delete_message(self, row):
        self.broadcast("TEST_DELETE", "--es", "uri", f"content://mms/{row['_id']}")

    def delete_thread(self, thread_id):
        self.broadcast("TEST_DELETE", "--el", "thread", str(thread_id))

    def retry(self, row):
        self.broadcast("TEST_RETRY", "--es", "uri", f"content://mms/{row['_id']}")

    def send_attachments(self, *to, attach, text=None):
        send_args = argparse.Namespace(
            to=list(to), text=text or f"send {uuid.uuid4().hex[:6]}", image=False, attach=attach,
            subscription=None, adb=self.args.adb, serial=self.args.serial, package=PACKAGE)
        tool.send(send_args)

    def downloaded(self, message_id):
        return wait_for("the downloaded message",
                        lambda: (r := self.row_for(message_id)) and r["m_type"] == "132" and r)

    def thread_of(self, row):
        rows = sql(self.device, f"select thread_id from pdu where _id={row['_id']}", ["thread_id"])
        return int(rows[0]["thread_id"])

    def open_thread(self, thread_id):
        self.device.adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
        self.device.adb("shell", "wm", "dismiss-keyguard", check=False)
        self.device.adb("shell", "cmd", "statusbar", "collapse", check=False)
        self.device.adb("shell", "am", "start", "-W", "-n",
                        f"{PACKAGE}/org.prauga.messages.feature.compose.ComposeActivity",
                        "--el", "threadId", str(thread_id))
        time.sleep(2)

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

def _receive_left_to_user(ctx, mode):
    set_scenario("download", mode)
    message_id = ctx.push()
    assert_not_retried(ctx, "download", r"MMS download result: MANUAL")
    row = ctx.row_for(message_id)
    assert row and row["m_type"] == str(M_TYPE_NOTIFICATION), f"notification should stay: {row}"
    assert row["st"] != str(PERMANENT_FAILURE), "it can still be downloaded"
    set_scenario("download", "ok")
    ctx.download(row)
    ctx.downloaded(message_id)

def receive_empty_response_left_to_user(ctx):
    _receive_left_to_user(ctx, "empty")

def receive_expired_status_is_permanent(ctx):
    set_scenario("download", "expired")
    message_id = ctx.push()
    wait_for("the expired notification",
             lambda: (r := ctx.row_for(message_id)) and r["st"] == str(PERMANENT_FAILURE))
    assert ctx.app_log(r"MMS download result: NEVER")

def receive_garbage_left_to_user(ctx):
    _receive_left_to_user(ctx, "garbage")

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

def _receive_parts(ctx, parts, expected_types, **overrides):
    message_id = ctx.push(parts=parts, **overrides)
    row = ctx.downloaded(message_id)
    types = [p["ct"] for p in parts_of(ctx.device, row["_id"])]
    for expected in expected_types:
        assert expected in types, f"{expected} missing from {types}"
    return row

def receive_jpeg(ctx):
    _receive_parts(ctx, "jpeg", ["image/jpeg"])

def receive_gif(ctx):
    _receive_parts(ctx, "gif", ["image/gif"])

def receive_video(ctx):
    _receive_parts(ctx, "video", ["video/mp4"])

def receive_audio(ctx):
    _receive_parts(ctx, "audio", ["audio/amr"])

def receive_vcard(ctx):
    _receive_parts(ctx, "vcard", ["text/x-vcard"])

def receive_pdf(ctx):
    _receive_parts(ctx, "pdf", ["application/pdf"])

def receive_many_attachments(ctx):
    _receive_parts(ctx, "png,jpeg,gif,video,audio,vcard,pdf",
                   ["image/png", "image/jpeg", "image/gif", "video/mp4", "audio/amr",
                    "text/x-vcard", "application/pdf", "text/plain"])

def receive_without_text(ctx):
    row = _receive_parts(ctx, "png", ["image/png"], no_text=True)
    types = [p["ct"] for p in parts_of(ctx.device, row["_id"])]
    assert "text/plain" not in types, types

def receive_with_subject(ctx):
    message_id = ctx.push(subject="Subject line", text="Body under a subject")
    row = ctx.downloaded(message_id)
    subject = sql(ctx.device, f"select sub from pdu where _id={row['_id']}", ["sub"])[0]["sub"]
    assert subject == "Subject line", subject

def receive_unicode_and_emoji(ctx):
    text = "नमस्ते 👋 こんにちは — ünïcødé"
    message_id = ctx.push(text=text)
    row = ctx.downloaded(message_id)
    parts = sql(ctx.device, f"select text from part where mid={row['_id']} and ct='text/plain'",
                ["text"])
    assert parts and parts[0]["text"] == text, parts

def receive_long_text(ctx):
    text = "long " * 400
    message_id = ctx.push(text=text)
    row = ctx.downloaded(message_id)
    parts = parts_of(ctx.device, row["_id"])
    assert any(p["ct"] == "text/plain" and int(p["text_length"] or 0) >= 1990 for p in parts), parts

def receive_large(ctx):
    _receive_parts(ctx, "big", ["image/png"])

def receive_broken_audio_and_vcard_dont_crash(ctx):
    row = _receive_parts(ctx, "badaudio,badvcard", ["audio/mpeg", "text/x-vcard"])
    ctx.open_thread(ctx.thread_of(row))
    time.sleep(3)
    assert not app_crashed(ctx.device), "the conversation crashed on a malformed part"

def receive_from_email_sender(ctx):
    message_id = ctx.push(sender="someone@example.com")
    row = ctx.downloaded(message_id)
    senders = [a["address"] for a in addresses_of(ctx.device, row["_id"]) if a["type"] == "137"]
    assert senders == ["someone@example.com"], senders

def receive_group(ctx):
    message_id = ctx.push(to=["+15555550100", "+15550001111", "+15550002222"], cc=["+15550003333"])
    row = ctx.downloaded(message_id)
    addresses = addresses_of(ctx.device, row["_id"])
    to = {a["address"] for a in addresses if a["type"] == "151"}
    cc = {a["address"] for a in addresses if a["type"] == "130"}
    assert {"+15550001111", "+15550002222"} <= to, addresses
    assert "+15550003333" in cc, addresses
    recipients = sql(ctx.device, f"select recipient_ids from threads where _id={ctx.thread_of(row)}",
                     ["recipient_ids"])[0]["recipient_ids"]
    assert len(recipients.split()) >= 3, f"not a group thread: {recipients}"

def receive_group_replies_land_in_same_thread(ctx):
    group = ["+15555550100", "+15550004444", "+15550005555"]
    first = ctx.downloaded(ctx.push(sender="+15550004444", to=group))
    second = ctx.downloaded(ctx.push(sender="+15550005555", to=group))
    assert ctx.thread_of(first) == ctx.thread_of(second), "a group reply started a new thread"

def receive_shows_notification(ctx):
    message_id = ctx.push(text="notify me please")
    ctx.downloaded(message_id)
    wait_for("a notification", lambda: any("notify me please" in t or "+1 555" in t
                                           for t in notification_texts(ctx.device)), timeout=20)

def receive_from_blocked_sender(ctx):
    sender = "+15556667777"
    ctx.block(sender)
    try:
        message_id = ctx.push(sender=sender, text="from a blocked number")
        row = wait_for("the notification row", lambda: ctx.row_for(message_id))
        time.sleep(5)
        downloads = [e for e in ctx.events("download") if e.get("id") == message_id]
        assert not downloads, "a blocked sender's MMS was downloaded automatically"
        assert row["m_type"] == str(M_TYPE_NOTIFICATION), row
        responses = ctx.events("notify-resp")
        assert responses and responses[-1]["status"] == hex(0x83), responses
        assert not any("from a blocked number" in t for t in notification_texts(ctx.device))
    finally:
        ctx.block(sender, unblock=True)

def delete_received_message(ctx):
    row = ctx.downloaded(ctx.push(parts="png"))
    ctx.delete_message(row)
    wait_for("the message to be deleted", lambda: not ctx.device.mms_rows(f"_id={row['_id']}"))
    assert not parts_of(ctx.device, row["_id"]), "its parts were left behind"

def delete_conversation(ctx):
    sender = "+15558889999"
    first = ctx.downloaded(ctx.push(sender=sender))
    ctx.downloaded(ctx.push(sender=sender))
    thread_id = ctx.thread_of(first)
    ctx.delete_thread(thread_id)
    wait_for("the conversation to be deleted",
             lambda: not sql(ctx.device, f"select _id from pdu where thread_id={thread_id}", ["_id"]))

def delete_waiting_notification(ctx):
    ctx.set_pref("mmsAutoDownload", False)
    try:
        message_id = ctx.push()
        row = wait_for("the waiting notification", lambda: ctx.row_for(message_id))
        time.sleep(2)
        ctx.delete_message(row)
        wait_for("the notification to be deleted", lambda: not ctx.row_for(message_id))
    finally:
        ctx.set_pref("mmsAutoDownload")

def open_image_in_gallery(ctx):
    row = ctx.downloaded(ctx.push(parts="jpeg", sender=f"+1555{uuid.uuid4().int % 10**7:07d}"))
    ctx.open_thread(ctx.thread_of(row))
    assert wait_for("the image in the conversation",
                    lambda: tap_view(ctx.device, "thumbnail"), timeout=10), "no image shown"
    wait_for("the gallery", lambda: "GalleryActivity" in resumed_activity(ctx.device), timeout=10)
    time.sleep(2)
    assert not app_crashed(ctx.device), "the gallery crashed"
    ctx.device.adb("shell", "input", "keyevent", "KEYCODE_BACK")

def open_conversation_with_every_kind(ctx):
    row = ctx.downloaded(ctx.push(parts="png,jpeg,gif,video,audio,vcard,pdf"))
    ctx.open_thread(ctx.thread_of(row))
    time.sleep(3)
    ctx.device.adb("shell", "input", "swipe", "540", "1500", "540", "600", "300")
    time.sleep(2)
    assert not app_crashed(ctx.device), "the conversation crashed"

def receive_while_app_is_killed(ctx):
    set_scenario("download", "slow")
    set_scenario("delay", 12)
    message_id = ctx.push()
    wait_for("the download to start", lambda: ctx.events("download"))
    pid = ctx.device.adb("shell", "pidof", PACKAGE, check=False).strip()
    assert pid, "the app isn't running"
    ctx.device.adb("shell", "kill", "-9", pid)
    ctx.downloaded(message_id)

def receive_many_at_once(ctx):
    ids = [ctx.push(sender=f"+155500077{i:02d}") for i in range(5)]
    for message_id in ids:
        ctx.downloaded(message_id)
    assert len(ctx.events("download")) == 5

def receive_anonymous_sender(ctx):
    message_id = ctx.push(sender="", text="who sent this")
    row = ctx.downloaded(message_id)
    ctx.open_thread(ctx.thread_of(row))
    time.sleep(2)

def receive_unknown_and_unnamed_parts(ctx):
    row = _receive_parts(ctx, "octet,noname", ["application/octet-stream", "image/png"])
    ctx.open_thread(ctx.thread_of(row))
    time.sleep(2)

def receive_subject_without_content(ctx):
    message_id = ctx.push(no_text=True, subject="Only a subject", parts="")
    row = ctx.downloaded(message_id)
    ctx.open_thread(ctx.thread_of(row))
    time.sleep(2)

def receive_spam_dropped(ctx):
    sender = "+15556660001"
    ctx.block(sender)
    ctx.set_pref("drop", True)
    try:
        message_id = ctx.push(sender=sender, text="spam spam spam")
        wait_for("the notification to arrive", lambda: ctx.app_log(r"Not downloading MMS automatically"))
        wait_for("the dropped message to be removed", lambda: not ctx.row_for(message_id), timeout=20)
        assert not ctx.events("download"), "spam was downloaded"
    finally:
        ctx.set_pref("drop")
        ctx.block(sender, unblock=True)

def receive_emoji_reaction(ctx):
    sender = f"+1555{uuid.uuid4().int % 10**7:07d}"
    ctx.downloaded(ctx.push(sender=sender, text="Dinner at eight?"))
    ctx.downloaded(ctx.push(sender=sender, text="Loved \u201cDinner at eight?\u201d"))
    wait_for("the reaction to be recognised", lambda: ctx.app_log(r"Reaction found with"), timeout=15)

def receive_with_mobile_data_off(ctx):
    ctx.device.adb("shell", "svc", "data", "disable")
    try:
        time.sleep(3)
        message_id = ctx.push()
        row = wait_for("the notification", lambda: ctx.row_for(message_id))
        wait_for("the failed download to be handled",
                 lambda: ctx.app_log(r"MMS download result: (MANUAL|AUTO)"), timeout=120)
        row = ctx.row_for(message_id)
        assert row and row["m_type"] == str(M_TYPE_NOTIFICATION), f"should still be waiting: {row}"
    finally:
        ctx.device.adb("shell", "svc", "data", "enable")
    time.sleep(8)
    ctx.download(row)
    ctx.downloaded(message_id)

def send_many_at_once(ctx):
    before = len(ctx.device.mms_rows("m_type=128 and msg_box=2"))
    for i in range(3):
        ctx.send_attachments(f"+1555000081{i}", attach="jpeg")
    wait_for("all three to be sent",
             lambda: len(ctx.device.mms_rows("m_type=128 and msg_box=2")) >= before + 3)
    assert len(ctx.events("send")) == 3, ctx.events("send")

def send_individually_with_attachment(ctx):
    send_args = argparse.Namespace(
        to=["+15550000901", "+15550000902"], text="each gets the photo", image=False,
        attach="jpeg", individually=True, subscription=None, adb=ctx.args.adb,
        serial=ctx.args.serial, package=PACKAGE)
    tool.send(send_args)
    sends = wait_for("both messages", lambda: len(ctx.events("send")) >= 2 and ctx.events("send"))
    assert all(send["bytes"] > 500 for send in sends), f"an attachment went missing: {sends}"

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

def _send_and_check(ctx, *to, attach, expected_types):
    ctx.send_attachments(*to, attach=attach)
    row = wait_for("the message to be sent",
                   lambda: (r := ctx.latest_outgoing()) and r["msg_box"] == str(BOX_SENT) and r)
    types = [p["ct"] for p in parts_of(ctx.device, row["_id"])]
    for expected in expected_types:
        assert expected in types, f"{expected} missing from {types}"
    return row

def send_jpeg(ctx):
    _send_and_check(ctx, "+15550000010", attach="jpeg", expected_types=["image/jpeg"])

def send_gif(ctx):
    _send_and_check(ctx, "+15550000011", attach="gif", expected_types=["image/gif"])

def send_video(ctx):
    _send_and_check(ctx, "+15550000012", attach="video", expected_types=["video/mp4"])

def send_audio_recording(ctx):
    _send_and_check(ctx, "+15550000013", attach="audio", expected_types=["audio/amr"])

def send_contact_card(ctx):
    _send_and_check(ctx, "+15550000014", attach="vcard", expected_types=["text/x-vcard"])

def send_file(ctx):
    _send_and_check(ctx, "+15550000015", attach="pdf", expected_types=["application/pdf"])

def send_many_attachments(ctx):
    _send_and_check(ctx, "+15550000016", attach="png,jpeg,gif,video,audio,vcard,pdf",
                    expected_types=["image/jpeg", "image/gif", "video/mp4",
                                    "audio/amr", "text/x-vcard", "application/pdf"])

def send_group_with_attachment(ctx):
    row = _send_and_check(ctx, "+15550000017", "+15550000018", "+15550000019", attach="jpeg",
                          expected_types=["image/jpeg"])
    to = [a["address"] for a in addresses_of(ctx.device, row["_id"]) if a["type"] == "151"]
    assert len(to) == 3, to

def send_large_image_is_compressed(ctx):
    _send_and_check(ctx, "+15550000020", attach="bigimage", expected_types=["image/jpeg"])
    limit = int(ctx.device.adb("shell", "dumpsys", "carrier_config", check=False)
                .split("maxMessageSize = ")[-1].split()[0] or 1048576)
    sends = ctx.events("send")
    assert sends[-1]["bytes"] <= limit, f"sent {sends[-1]['bytes']} bytes, over the {limit} limit"

def send_too_large_file_fails(ctx):
    ctx.send_attachments("+15550000021", attach="bigfile")
    row = wait_for("the message to fail",
                   lambda: (r := ctx.latest_outgoing()) and r["msg_box"] == str(BOX_FAILED) and r)
    assert not ctx.events("send"), "an oversized message reached the MMSC"
    assert ctx.app_log(r"more than the \d+ the carrier accepts")

def send_to_existing_conversation(ctx):
    first = _send_and_check(ctx, "+15550000022", "+15550000023", attach="png",
                            expected_types=["image/jpeg"])
    second = _send_and_check(ctx, "+15550000022", "+15550000023", attach="jpeg",
                             expected_types=["image/jpeg"])
    assert ctx.thread_of(first) == ctx.thread_of(second), "the same recipients got a new thread"

def send_failed_then_retried_by_hand(ctx):
    set_scenario("send", "permanent")
    ctx.send("+15550000024", "+15550000025")
    row = wait_for("the message to fail",
                   lambda: (r := ctx.latest_outgoing()) and r["msg_box"] == str(BOX_FAILED) and r)
    set_scenario("send", "ok")
    time.sleep(2)
    ctx.retry(row)
    wait_for("the retried message to be sent",
             lambda: (r := ctx.device.mms_rows(f"_id={row['_id']}")) and r[0]["msg_box"] == str(BOX_SENT))

def delivery_report_for_sent_message(ctx):
    stored = len(ctx.device.mms_rows("m_type=134"))
    ctx.send("+15550000026", "+15550000027")
    row = wait_for("the message to be sent",
                   lambda: (r := ctx.latest_outgoing()) and r["msg_box"] == str(BOX_SENT) and r)
    report_args = argparse.Namespace(kind="delivery", message_id=row["m_id"], recipient="+15550000026",
                                     status="delivered", subscription=None, adb=ctx.args.adb,
                                     serial=ctx.args.serial, package=PACKAGE)
    tool.report(report_args)
    time.sleep(5)
    assert not app_crashed(ctx.device), "a delivery report crashed the app"
    assert len(ctx.device.mms_rows("m_type=134")) == stored, "the delivery report was stored"

def read_report_for_sent_message(ctx):
    stored = len(ctx.device.mms_rows("m_type=136"))
    ctx.send("+15550000028", "+15550000029")
    row = wait_for("the message to be sent",
                   lambda: (r := ctx.latest_outgoing()) and r["msg_box"] == str(BOX_SENT) and r)
    report_args = argparse.Namespace(kind="read", message_id=row["m_id"], recipient="+15550000028",
                                     status="read", subscription=None, adb=ctx.args.adb,
                                     serial=ctx.args.serial, package=PACKAGE)
    tool.report(report_args)
    time.sleep(5)
    assert not app_crashed(ctx.device), "a read report crashed the app"
    assert len(ctx.device.mms_rows("m_type=136")) == stored, "the read report was stored"

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
    receive_jpeg,
    receive_gif,
    receive_video,
    receive_audio,
    receive_vcard,
    receive_pdf,
    receive_many_attachments,
    receive_without_text,
    receive_with_subject,
    receive_unicode_and_emoji,
    receive_long_text,
    receive_large,
    receive_broken_audio_and_vcard_dont_crash,
    receive_from_email_sender,
    receive_group,
    receive_group_replies_land_in_same_thread,
    receive_shows_notification,
    receive_from_blocked_sender,
    delete_received_message,
    delete_conversation,
    delete_waiting_notification,
    open_image_in_gallery,
    open_conversation_with_every_kind,
    receive_while_app_is_killed,
    receive_many_at_once,
    receive_anonymous_sender,
    receive_unknown_and_unnamed_parts,
    receive_subject_without_content,
    receive_spam_dropped,
    receive_emoji_reaction,
    receive_with_mobile_data_off,
    receive_duplicate_push,
    receive_404_keeps_expired_message,
    receive_server_error_retries,
    receive_bad_request_retries,
    receive_transient_status_retries,
    receive_empty_response_left_to_user,
    receive_expired_status_is_permanent,
    receive_garbage_left_to_user,
    receive_auto_download_off_then_manual,
    receive_pending_downloaded_when_app_opens,
    send_ok,
    send_with_image,
    send_jpeg,
    send_gif,
    send_video,
    send_audio_recording,
    send_contact_card,
    send_file,
    send_many_attachments,
    send_group_with_attachment,
    send_large_image_is_compressed,
    send_too_large_file_fails,
    send_to_existing_conversation,
    send_failed_then_retried_by_hand,
    send_many_at_once,
    send_individually_with_attachment,
    delivery_report_for_sent_message,
    read_report_for_sent_message,
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
        device.adb("logcat", "-b", "crash", "-c", check=False)
        try:
            scenario(ctx)
            assert not app_crashed(device), "the app crashed"
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
