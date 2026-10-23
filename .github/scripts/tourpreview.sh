#!/usr/bin/env bash
# Opens the debug-only TourPreviewActivity (the "what is new" tour on its own) in an emulator and takes a screenshot of
# each of its three pages. Run through .github/workflows/smoke-test.yml with script=tourpreview.sh.
set -u

PKG=com.sualtikasifi.cizimhafiza
OUT=smoke-out
mkdir -p "$OUT"
APK=$(ls cizim-hafiza/app/build/outputs/apk/debug/*.apk | head -1)
adb wait-for-device
adb shell input keyevent 82 || true
adb install -r -t "$APK"
adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.TourPreviewActivity"
sleep 6
adb exec-out screencap -p > "$OUT/tour_1.png"
# the orange button sits at about 79% of the screen height
for n in 2 3; do
  adb shell input tap 540 1900
  sleep 2
  adb exec-out screencap -p > "$OUT/tour_$n.png"
done
ls -la "$OUT"
