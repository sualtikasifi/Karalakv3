#!/usr/bin/env bash
# Opens the debug-only ResultStatsPreviewActivity (the level result stat cards) in an emulator and takes a screenshot.
# Run through .github/workflows/smoke-test.yml with script=resultstatspreview.sh.
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
adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.ResultStatsPreviewActivity"
sleep 8
adb exec-out screencap -p > "$OUT/result_stats.png"
ls -la "$OUT"
