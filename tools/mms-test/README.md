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
