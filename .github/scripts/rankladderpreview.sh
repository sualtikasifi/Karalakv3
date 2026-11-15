#!/usr/bin/env bash
# Opens the debug-only RankLadderPreviewActivity (the painted rank ladder) at three player levels and takes a screenshot of each.
# Run through .github/workflows/smoke-test.yml with script=rankladderpreview.sh.
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
for lv in 3 79 100; do
  adb shell am force-stop "$PKG"
  adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.RankLadderPreviewActivity" --ei level "$lv"
  sleep 4
  adb exec-out screencap -p > "$OUT/rank_ladder_$lv.png"
done
adb logcat -d | grep -E "FATAL|AndroidRuntime" | head -40 > "$OUT/crash.txt" || true
ls -la "$OUT"
