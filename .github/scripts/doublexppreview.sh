#!/usr/bin/env bash
# Opens the debug-only DoubleXpPreviewActivity (the result screen's "x2 XP" ad button beside "Ödülü Al") and takes a screenshot at two XP amounts.
# Run through .github/workflows/smoke-test.yml with script=doublexppreview.sh.
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
for xp in 171 5230; do
  adb shell am force-stop "$PKG"
  adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.DoubleXpPreviewActivity" --ei xp "$xp"
  sleep 4
  adb exec-out screencap -p > "$OUT/double_xp_$xp.png"
done
adb logcat -d | grep -E "FATAL|AndroidRuntime" | head -40 > "$OUT/crash.txt" || true
ls -la "$OUT"
