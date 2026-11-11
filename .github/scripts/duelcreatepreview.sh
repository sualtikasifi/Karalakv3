#!/usr/bin/env bash
# Opens the debug-only DuelCreatePreviewActivity (the challenge page) with a normal and a very long opponent name,
# takes a screenshot of each. Run through .github/workflows/smoke-test.yml with script=duelcreatepreview.sh.
set -u

PKG=com.sualtikasifi.cizimhafiza
OUT=smoke-out
mkdir -p "$OUT"
APK=$(ls cizim-hafiza/app/build/outputs/apk/debug/*.apk | head -1)
adb wait-for-device
adb shell input keyevent 82 || true
adb install -r -t "$APK"
adb shell settings put global hide_error_dialogs 1 || true
for name in "Bob Ross" "Maximilian Alexander von Hohenzollern"; do
  tag=$(echo "$name" | tr -cd 'A-Za-z' | cut -c1-8)
  adb shell am force-stop "$PKG"
  adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.DuelCreatePreviewActivity" --es name "$name"
  sleep 3
  adb exec-out screencap -p > "$OUT/duelcreate_${tag}.png"
  adb shell input tap 540 1900
  sleep 1
  adb exec-out screencap -p > "$OUT/duelcreate_${tag}_tap.png"
done
adb logcat -d | grep -E "FATAL|AndroidRuntime" | head -40 > "$OUT/crash.txt" || true
ls -la "$OUT"
