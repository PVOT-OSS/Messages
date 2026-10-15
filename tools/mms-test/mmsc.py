#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
# SPDX-License-Identifier: GPL-3.0-or-later

"""
A fake MMSC for testing MMS end to end on the Android emulator, without a carrier.

  ./mmsc.py serve                      run the MMSC (port 8580, reachable from the emulator
                                       as http://10.0.2.2:8580)
  ./mmsc.py push [options]             deliver an MMS notification to the emulator through its
                                       modem, the way a carrier does, so the app downloads it
  ./mmsc.py scenario NAME [VALUE]      change how the MMSC answers, e.g. "send transient"
  ./mmsc.py log                        show what the MMSC received (sends, downloads, acks)

The emulator console is used to inject the WAP push (`sms pdu`), and the download and send
requests made by Android's MmsService come to this server over the emulator's mobile data.
"""

import argparse
import fcntl
import json
import socket
import subprocess
import sys
import threading
import time
import uuid
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlparse

import mmspdu

PORT = 8580
HOST_FROM_EMULATOR = "10.0.2.2"
STATE = Path(__file__).with_name(".mmsc-state.json")

DEFAULT_SCENARIO = {
    "download": "ok",
    "send": "ok",
    "delay": 0,
}

def load_state():
    if STATE.exists():
        return json.loads(STATE.read_text())
    return {"scenario": dict(DEFAULT_SCENARIO), "messages": {}, "log": []}

def save_state(state):
    STATE.write_text(json.dumps(state, indent=2))

class StateLock:

    def __init__(self):
        self.thread_lock = threading.Lock()

    def __enter__(self):
        self.thread_lock.acquire()
        self.file = open(STATE.with_suffix(".lock"), "w")
        fcntl.flock(self.file, fcntl.LOCK_EX)

    def __exit__(self, *args):
        fcntl.flock(self.file, fcntl.LOCK_UN)
        self.file.close()
        self.thread_lock.release()

lock = StateLock()

def record(state, entry):
    entry["time"] = time.strftime("%H:%M:%S")
    state["log"].append(entry)
    print(json.dumps(entry), flush=True)

class Mmsc(BaseHTTPRequestHandler):
    def log_message(self, *args):
        pass

    def reply(self, status, body=b""):
        self.send_response(status)
        self.send_header("Content-Type", "application/vnd.wap.mms-message")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        with lock:
            state = load_state()
            scenario = state["scenario"]
            message_id = urlparse(self.path).path.rstrip("/").split("/")[-1]
            message = state["messages"].get(message_id)
            mode = scenario["download"]
            record(state, {"event": "download", "id": message_id, "mode": mode,
                           "ua": self.headers.get("User-Agent"),
                           "profile": self.headers.get("x-wap-profile")})
            save_state(state)

        if mode == "slow":
            time.sleep(scenario.get("delay") or 30)
        elif scenario.get("delay"):
            time.sleep(scenario["delay"])

        if message is None or mode == "404":
            return self.reply(404)
        if mode in ("400", "500"):
            return self.reply(int(mode))
        if mode == "empty":
            return self.reply(200)
        if mode == "garbage":
            return self.reply(200, b"\x01\x02not an mms")

        status = {"transient": mmspdu.RETRIEVE_TRANSIENT_FAILURE,
                  "expired": mmspdu.RETRIEVE_PERMANENT_MESSAGE_NOT_FOUND}.get(mode)
        parts = None
        if message.get("parts") is not None:
            parts = [mmspdu.PART_KINDS[kind]() for kind in message["parts"]]
            if message.get("text") is not None:
                parts.insert(0, ("text/plain", "text_0.txt", message["text"].encode("utf-8")))
        image = mmspdu.png_1x1() if message.get("image") else None
        body = mmspdu.retrieve_conf(message["transaction_id"], message["from"], message["to"],
                                    message.get("text"), image, status, parts=parts,
                                    subject=message.get("subject"), cc=message.get("cc"))
        self.reply(200, body)

    def do_POST(self):
        length = int(self.headers.get("Content-Length") or 0)
        pdu = self.rfile.read(length)
        headers = mmspdu.read_headers(pdu)
        kind = headers.get(mmspdu.MESSAGE_TYPE)
        transaction = (headers.get(mmspdu.TRANSACTION_ID) or ["?"])[0]

        with lock:
            state = load_state()
            scenario = state["scenario"]
            names = {mmspdu.SEND_REQ: "send", mmspdu.NOTIFYRESP_IND: "notify-resp",
                     mmspdu.ACKNOWLEDGE_IND: "acknowledge"}
            entry = {"event": names.get(kind, f"unknown-{kind}"), "transaction": transaction,
                     "bytes": len(pdu), "ua": self.headers.get("User-Agent")}
            if kind == mmspdu.SEND_REQ:
                entry["to"] = headers.get(mmspdu.TO, [])
                entry["mode"] = scenario["send"]
            if kind == mmspdu.NOTIFYRESP_IND:
                entry["status"] = hex(headers.get(0x95, 0))
            record(state, entry)
            save_state(state)

        if kind != mmspdu.SEND_REQ:
            return self.reply(204)

        mode = scenario["send"]
        if mode == "slow":
            time.sleep(scenario.get("delay") or 30)
        elif scenario.get("delay"):
            time.sleep(scenario["delay"])

        if mode in ("404", "500"):
            return self.reply(int(mode))
        if mode == "garbage":
            return self.reply(200, b"\x00\x01")

        status = {"ok": mmspdu.RESPONSE_OK,
                  "transient": mmspdu.RESPONSE_TRANSIENT_FAILURE,
                  "network": mmspdu.RESPONSE_TRANSIENT_NETWORK_PROBLEM,
                  "permanent": mmspdu.RESPONSE_PERMANENT_FAILURE,
                  "denied": mmspdu.RESPONSE_PERMANENT_SERVICE_DENIED}.get(mode, mmspdu.RESPONSE_OK)
        self.reply(200, mmspdu.send_conf(transaction, status, f"msg-{uuid.uuid4().hex[:8]}"))

def serve(args):
    if not STATE.exists():
        save_state(load_state())
    server = ThreadingHTTPServer(("0.0.0.0", args.port), Mmsc)
    print(f"Fake MMSC on port {args.port}, emulator URL http://{HOST_FROM_EMULATOR}:{args.port}/mms",
          flush=True)
    server.serve_forever()

def console(commands, port):
    token = (Path.home() / ".emulator_console_auth_token").read_text().strip()
    with socket.create_connection(("127.0.0.1", port), timeout=10) as sock:
        sock.settimeout(5)

        def send(line):
            sock.sendall((line + "\n").encode())
            time.sleep(0.3)
            try:
                return sock.recv(65536).decode(errors="replace")
            except socket.timeout:
                return ""

        time.sleep(0.3)
        sock.recv(65536)
        send(f"auth {token}")
        return [send(command) for command in commands]

def push(args):
    with lock:
        state = load_state()
        message_id = args.id or uuid.uuid4().hex[:8]
        transaction = args.transaction or f"T{message_id}"
        state["messages"][message_id] = {
            "transaction_id": transaction, "from": args.sender,
            "to": args.to if len(args.to) > 1 else args.to[0],
            "text": None if args.no_text else args.text, "image": args.image,
            "parts": [p for p in args.parts.split(",") if p] if args.parts is not None else None,
            "subject": args.subject, "cc": args.cc,
        }
        record(state, {"event": "push", "id": message_id, "from": args.sender})
        save_state(state)

    location = f"http://{args.host}:{args.port}/mms/{message_id}"
    pdu = mmspdu.notification_ind(transaction, args.sender, location)
    deliver_push(pdu, args)
    print(f"pushed {message_id} -> {location}")

def deliver_push(pdu: bytes, args):
    command = [*adb(args), "shell", "am", "broadcast",
               "-a", "org.prauga.messages.TEST_MMS_PUSH",
               "-n", f"{args.package}/org.prauga.messages.receiver.TestMmsPushReceiver",
               "--es", "pdu", pdu.hex()]
    if args.subscription is not None:
        command += ["--ei", "subscription", str(args.subscription)]
    result = subprocess.run(command, capture_output=True, text=True)
    if result.returncode != 0 or "Broadcast completed" not in result.stdout:
        print(result.stdout, result.stderr, file=sys.stderr)

def send(args):
    command = [*adb(args), "shell", "am", "broadcast",
               "-a", "org.prauga.messages.TEST_MMS_SEND",
               "-n", f"{args.package}/org.prauga.messages.receiver.TestMmsPushReceiver",
               "--es", "to", ",".join(args.to),
               "--es", "text", f"'{args.text}'"]
    if args.image:
        command += ["--ez", "image", "true"]
    if getattr(args, "attach", None):
        command += ["--es", "attach", args.attach]
    if getattr(args, "individually", False):
        command += ["--ez", "individually", "true"]
    if args.subscription is not None:
        command += ["--ei", "subscription", str(args.subscription)]
    result = subprocess.run(command, capture_output=True, text=True)
    if result.returncode != 0 or "Broadcast completed" not in result.stdout:
        print(result.stdout, result.stderr, file=sys.stderr)
    print(f"asked the app to send to {', '.join(args.to)}")

def adb(args):
    command = [args.adb]
    if args.serial:
        command += ["-s", args.serial]
    return command

def report(args):
    if args.kind == "delivery":
        status = {"delivered": 0x81, "rejected": 0x83, "expired": 0x80}[args.status]
        pdu = mmspdu.delivery_ind(args.message_id, args.recipient, status)
    else:
        pdu = mmspdu.read_orig_ind(args.message_id, args.recipient, args.status != "deleted")
    deliver_push(pdu, args)
    print(f"pushed a {args.kind} report for {args.message_id}")

def scenario(args):
    with lock:
        state = load_state()
        if args.name == "reset":
            state["scenario"] = dict(DEFAULT_SCENARIO)
        elif args.name == "delay":
            state["scenario"]["delay"] = int(args.value)
        else:
            state["scenario"][args.name] = args.value
        save_state(state)
    print(json.dumps(state["scenario"]))

def show_log(args):
    state = load_state()
    for entry in state["log"][-args.tail:]:
        print(json.dumps(entry))
    if args.clear:
        state["log"] = []
        save_state(state)

def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawTextHelpFormatter)
    parser.add_argument("--port", type=int, default=PORT)
    parser.add_argument("--console", type=int, default=5554, help="emulator console port")
    parser.add_argument("--adb", default="adb")
    parser.add_argument("--serial", default="emulator-5554", help="adb device to use")
    parser.add_argument("--package", default="org.prauga.messages.debug")
    parser.add_argument("--host", default=HOST_FROM_EMULATOR,
                        help="how the device reaches this machine")
    sub = parser.add_subparsers(dest="command", required=True)

    sub.add_parser("serve").set_defaults(func=serve)

    p = sub.add_parser("push")
    p.add_argument("--sender", default="+15551234567")
    p.add_argument("--to", nargs="+", default=["+15555215554"],
                   help="recipients, include others for a group message")
    p.add_argument("--cc", nargs="*", default=[])
    p.add_argument("--text", default="Hello from the fake MMSC")
    p.add_argument("--no-text", action="store_true")
    p.add_argument("--subject")
    p.add_argument("--image", action="store_true", help="attach a small image")
    p.add_argument("--parts", help=f"comma separated: {', '.join(mmspdu.PART_KINDS)}")
    p.add_argument("--id", help="message id, reuse one to send a duplicate push")
    p.add_argument("--transaction")
    p.add_argument("--subscription", type=int)
    p.set_defaults(func=push)

    p = sub.add_parser("send")
    p.add_argument("to", nargs="+", help="recipients, several make it a group MMS")
    p.add_argument("--text", default="Hello fake MMSC")
    p.add_argument("--image", action="store_true")
    p.add_argument("--attach", help="comma separated: png jpeg bigimage gif video audio vcard pdf bigfile")
    p.add_argument("--individually", action="store_true", help="one message per recipient")
    p.add_argument("--subscription", type=int)
    p.set_defaults(func=send)

    p = sub.add_parser("report")
    p.add_argument("kind", choices=["delivery", "read"])
    p.add_argument("message_id", help="the m_id the MMSC gave the sent message")
    p.add_argument("--recipient", default="+15550000001")
    p.add_argument("--status", default="delivered",
                   help="delivery: delivered rejected expired, read: read deleted")
    p.add_argument("--subscription", type=int)
    p.set_defaults(func=report)

    p = sub.add_parser("scenario")
    p.add_argument("name", help="download | send | delay | reset")
    p.add_argument("value", nargs="?")
    p.set_defaults(func=scenario)

    p = sub.add_parser("log")
    p.add_argument("--tail", type=int, default=30)
    p.add_argument("--clear", action="store_true")
    p.set_defaults(func=show_log)

    args = parser.parse_args()
    args.func(args)

if __name__ == "__main__":
    main()
