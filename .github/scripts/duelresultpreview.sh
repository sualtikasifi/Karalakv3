#!/usr/bin/env bash
# Opens the debug-only DuelResultPreviewActivity (an answered duel's result screen), then switches the gallery to the challenger's drawings.
# Run through .github/workflows/smoke-test.yml with script=duelresultpreview.sh.
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
adb shell am force-stop "$PKG"
adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.DuelResultPreviewActivity"
sleep 5
adb exec-out screencap -p > "$OUT/duel_result_1.png"
# The challenger's chip in the gallery header (1080x2400 emulator); the UI tree also finds "liliput" on the versus card above, so no search.
adb shell input tap 707 1477
sleep 2
adb exec-out screencap -p > "$OUT/duel_result_2.png"
adb logcat -d | grep -E "FATAL|AndroidRuntime" | head -40 > "$OUT/crash.txt" || true
ls -la "$OUT"
