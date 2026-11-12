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
for combo in silver:none:0 gold:one:0 rare:many:0 gold:one:1 rare:many:1; do
  IFS=: read -r tier extras dbl <<< "$combo"
  adb shell am force-stop "$PKG"
  adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.ChestRewardPreviewActivity" --es tier "$tier" --es extras "$extras" --es double "$dbl"
  sleep 3
  adb exec-out screencap -p > "$OUT/reward_${tier}_${extras}_d${dbl}_0video.png"
  adb shell input tap 540 1200
  sleep 0.6
  adb exec-out screencap -p > "$OUT/reward_${tier}_${extras}_d${dbl}_1entrance.png"
  sleep 5
  adb exec-out screencap -p > "$OUT/reward_${tier}_${extras}_d${dbl}_2settled.png"
  if [ "$dbl" = "1" ]; then adb shell input tap 540 1760; sleep 3; adb exec-out screencap -p > "$OUT/reward_${tier}_${extras}_d${dbl}_3doubled.png"; fi
done
adb logcat -d | grep -E "FATAL|AndroidRuntime" | head -40 > "$OUT/crash.txt" || true
ls -la "$OUT"
