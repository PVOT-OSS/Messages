#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
# SPDX-License-Identifier: GPL-3.0-or-later

set -euo pipefail

SERIAL=${1:?usage: $0 SERIAL [--restore]}
PACKAGE=org.prauga.messages.debug
PORT=8580
STATE_DIR="$(dirname "$0")/.device-state/$SERIAL"
ADB=(adb -s "$SERIAL")
APK="$(dirname "$0")/../../presentation/build/outputs/apk/debug"

apn_field() {
    python3 -c 'import re, sys
row = open(sys.argv[1]).read()
match = re.search(r"(?:^|, |Row: \d+ )" + sys.argv[2] + r"=(.*?)(?=, \w+=|$)", row.strip())
print(match.group(1) if match else "")' "$STATE_DIR/apn.txt" "$1"
}

if [[ "${2:-}" == "--restore" ]]; then
    id=$(apn_field _id)
    "${ADB[@]}" shell "content update --uri content://telephony/carriers \
        --bind 'type:s:$(apn_field type)' \
        --bind 'mmsc:s:$(apn_field mmsc | sed 's/:/\\\\:/g')' --where '_id=$id'"
    "${ADB[@]}" shell cmd role add-role-holder android.app.role.SMS "$(cat "$STATE_DIR/sms_role.txt")"
    "${ADB[@]}" reverse --remove "tcp:$PORT" || true
    "${ADB[@]}" shell svc data disable && sleep 2 && "${ADB[@]}" shell svc data enable
    echo "restored $SERIAL"
    exit 0
fi

"${ADB[@]}" root >/dev/null && sleep 2 && "${ADB[@]}" wait-for-device
mkdir -p "$STATE_DIR"

if [[ ! -f "$STATE_DIR/apn.txt" ]]; then
    "${ADB[@]}" shell content query --uri content://telephony/carriers/preferapn > "$STATE_DIR/apn.txt"
    "${ADB[@]}" shell cmd role get-role-holders android.app.role.SMS > "$STATE_DIR/sms_role.txt"
fi

id=$(apn_field _id)
type=$(apn_field type)
[[ ",$type," == *",mms,"* || -z "$type" || "$type" == "*" ]] || type="$type,mms"
"${ADB[@]}" shell "content update --uri content://telephony/carriers \
    --bind 'type:s:$type' --bind 'mmsc:s:http\\://127.0.0.1\\:$PORT/mms' --where '_id=$id'"

"${ADB[@]}" shell svc data disable && sleep 2 && "${ADB[@]}" shell svc data enable && sleep 6

"${ADB[@]}" shell cmd phone cc set-value -p enabledMMS true >/dev/null 2>&1 || true

"${ADB[@]}" reverse "tcp:$PORT" "tcp:$PORT" >/dev/null
"${ADB[@]}" install -r "$APK"/*.apk >/dev/null
"${ADB[@]}" shell cmd role add-role-holder android.app.role.SMS "$PACKAGE"

echo "ready: $SERIAL"
echo "  run the MMSC:       ./mmsc.py serve"
echo "  run the scenarios:  ./scenarios.py --serial $SERIAL --host 127.0.0.1"
echo "  undo:               $0 $SERIAL --restore"
