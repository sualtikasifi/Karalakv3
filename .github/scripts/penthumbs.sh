#!/usr/bin/env bash
# Opens the debug-only PenThumbsPreviewActivity (the end-of-game drawings board in different pens) and shoots the first
# and the last pens. Run through .github/workflows/smoke-test.yml with script=penthumbs.sh.
set -u

PKG=com.sualtikasifi.cizimhafiza
OUT=smoke-out
mkdir -p "$OUT"
APK=$(ls cizim-hafiza/app/build/outputs/apk/debug/*.apk | head -1)
adb wait-for-device
adb shell input keyevent 82 || true
adb shell settings put global hide_error_dialogs 1 || true
sleep 20
adb install -r -t "$APK"
for first in 0 10 20; do
  adb shell am force-stop "$PKG"
  adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.PenThumbsPreviewActivity" --ei first "$first"
  sleep 6
  adb exec-out screencap -p > "$OUT/pens_$first.png"
done
ls -la "$OUT"
