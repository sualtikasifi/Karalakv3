#!/usr/bin/env bash
# Opens the debug-only UpdatePreviewActivity (the forced-update card on its own) in an emulator and takes a screenshot.
# Run through .github/workflows/smoke-test.yml with script=updatepreview.sh.
set -u

PKG=com.sualtikasifi.cizimhafiza
OUT=smoke-out
mkdir -p "$OUT"
APK=$(ls cizim-hafiza/app/build/outputs/apk/debug/*.apk | head -1)
adb wait-for-device
adb shell input keyevent 82 || true
adb install -r -t "$APK"
adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.UpdatePreviewActivity"
sleep 6
adb exec-out screencap -p > "$OUT/update_card.png"
# the back key must leave the card (it never lets the game show through)
adb shell input keyevent KEYCODE_BACK
sleep 2
adb exec-out screencap -p > "$OUT/after_back.png"
ls -la "$OUT"
