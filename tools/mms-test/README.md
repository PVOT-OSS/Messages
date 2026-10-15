# End to end MMS tests without a carrier

These tools test MMS sending and receiving for real, through Android's own `MmsService`, against a
fake MMSC running on your computer. No MMS-capable carrier is needed, and nothing is sent to the
carrier: the MMSC is reached at `127.0.0.1` on the device, forwarded to your computer over USB.

## What's real and what's simulated

| Step | |
|---|---|
| Carrier delivering the MMS notification (WAP push) | simulated: `adb` hands it to a debug-only receiver, which passes it to the app exactly like Android does |
| The app handling the push, download decisions, duplicates, SIMs | real |
| `MmsService` downloading and sending over the mobile data network | real |
| The MMSC answering downloads and sends | fake, scriptable (`mmsc.py`) |
| The app storing, acknowledging, retrying and showing messages | real |

The debug receiver (`data/src/debug`) only exists in debug builds and only accepts broadcasts from
the adb shell (it requires the `DUMP` permission).

## Requirements

- A device with a SIM and mobile data, or an emulator, where `adb root` works: a `userdebug` build,
  or an emulator with a **Google APIs** image (not Google Play).
- Python 3.10+.
- A debug build: `./gradlew assembleDebug`.

## Running

```sh
cd tools/mms-test
./mmsc.py serve                            # keep this running
./setup.sh <serial>                        # points the APN at the fake MMSC, installs the app
./scenarios.py --serial <serial> --host 127.0.0.1
./setup.sh <serial> --restore              # puts the APN and default SMS app back
```

`setup.sh` remembers the original APN and default SMS app in `.device-state/` the first time it
runs, and `--restore` puts them back.

## Trying things by hand

```sh
./mmsc.py --serial <serial> --host 127.0.0.1 push --text "Hi" --image   # receive an MMS
./mmsc.py --serial <serial> send +15550001 +15550002 --text "Hi"        # send a group MMS
./mmsc.py scenario download 404      # make downloads fail: ok 404 400 500 empty garbage transient expired slow
./mmsc.py scenario send transient    # make sends fail: ok transient network permanent denied 404 500 garbage slow
./mmsc.py scenario delay 10          # slow every answer down
./mmsc.py scenario reset
./mmsc.py log                        # what the MMSC saw: downloads, sends, NotifyResp, Acknowledge
```

## Scenarios

`scenarios.py` checks, for each case, what the MMSC received, what the app logged and what ended up
in the telephony provider:

- receiving: success with an image, duplicate pushes, 404 (kept as expired), 500 / 400 / transient
  errors / empty responses (retried automatically), expired and malformed responses (not retried),
  auto-download turned off (deferred, then downloaded by hand), and pending messages downloaded when
  the app opens
- sending: group and image messages, transient and server errors (retried automatically), and
  rejections, denials and 404s (failed without retrying)

## SMS scenarios (emulator only)

`sms_scenarios.py` runs SMS end to end on an **emulator**, whose modem really sends, receives and
reports delivery of SMS for free. It refuses any other device: on a phone every scenario would send
real SMS. Install the debug build and make it the default SMS app (no fake MMSC or setup.sh needed):

```sh
adb -s emulator-5554 install -r -g presentation/build/outputs/apk/debug/*-debug.apk
adb -s emulator-5554 shell cmd role add-role-holder android.app.role.SMS org.prauga.messages.debug
./sms_scenarios.py --adb ~/platform-tools/adb --serial emulator-5554 [NAME ...]
```

Results the modem can't produce (one part of a long SMS failing, a network error with a given
cause, a failed delivery report) are handed to the app's real receivers by the debug-only
`TEST_SMS_RESULT` action. Rows are read through `TEST_SMS_DUMP`, as the app sees them, since the
provider hides outbox and failed SMS from the shell.

- receiving: plain, long (multipart), non-Latin text, alphanumeric senders, and through
  `TEST_SMS_RECEIVE` with crafted PDUs: what the network sent is stored (protocol, reply path,
  service center), a replace SMS takes the place of the last one, class 0 pops up to save or dismiss
- sending: short and long SMS, delivery reports from the network
- long SMS: it's decided once every part reported, by the worst of them,
  whatever order they come in; results from before the update still work
- delivery reports: 0 delivered, 64 failed, anything else or no report at all changes nothing
- automatic retries: no service and temporary network errors are sent again by themselves, other
  errors fail and notify, airplane mode keeps it sending until the radio is back, and getting
  service back sends it right away instead of after its delay
- delayed sending: goes at its time, cancel, send now (same message, once), and neither does
  anything once it already went
- retry from the notification: sends a failed message once, nothing for a sent one
- reboot: a delayed SMS whose time passed while off goes after the boot, one still to come goes at
  its time

Run it once with exact alarm access (`adb shell appops set org.prauga.messages.debug
SCHEDULE_EXACT_ALARM allow`) and once without (`... deny`, the default for new installs since
Android 14), since delayed sending takes a different path for each. The emulator console can't send
characters beyond the BMP, like emoji.
