#!/usr/bin/env bash
# Opens the debug-only ChestRewardPreviewActivity (the chest-opening reward board) for each tier and amount of extras,
# taps once to skip the video, and takes a screenshot mid-entrance and one once it has settled.
# Run through .github/workflows/smoke-test.yml with script=chestrewardpreview.sh.
set -u

PKG=com.sualtikasifi.cizimhafiza
OUT=smoke-out
mkdir -p "$OUT"
APK=$(ls cizim-hafiza/app/build/outputs/apk/debug/*.apk | head -1)
adb wait-for-device
adb shell input keyevent 82 || true
adb install -r -t "$APK"
adb shell settings put global hide_error_dialogs 1 || true
for combo in gold:one rare:many silver:none gold:many; do
  tier=${combo%%:*}; extras=${combo##*:}
  adb shell am force-stop "$PKG"
  adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.ChestRewardPreviewActivity" --es tier "$tier" --es extras "$extras"
  sleep 3
  adb exec-out screencap -p > "$OUT/reward_${tier}_${extras}_0video.png"
  adb shell input tap 540 1200
  sleep 0.6
  adb exec-out screencap -p > "$OUT/reward_${tier}_${extras}_1entrance.png"
  sleep 5
  adb exec-out screencap -p > "$OUT/reward_${tier}_${extras}_2settled.png"
done
adb logcat -d | grep -E "FATAL|AndroidRuntime" | head -40 > "$OUT/crash.txt" || true
ls -la "$OUT"
