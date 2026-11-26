#!/usr/bin/env bash
# Opens the debug-only ChestPreviewActivity in its gift states: the send window, the blocked message and the gift box.
# Run through .github/workflows/smoke-test.yml with script=gift.sh.
set -u

PKG=com.sualtikasifi.cizimhafiza
OUT=smoke-out
mkdir -p "$OUT"
APK=$(ls cizim-hafiza/app/build/outputs/apk/debug/*.apk | head -1)
adb wait-for-device
adb shell input keyevent 82 || true
adb install -r -t "$APK"
for state in gift gift_blocked gift_inbox; do
  adb shell am force-stop "$PKG"
  adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.ChestPreviewActivity" --es tier silver --es state "$state"
  sleep 5
  adb exec-out screencap -p > "$OUT/${state}.png"
done
ls -la "$OUT"
