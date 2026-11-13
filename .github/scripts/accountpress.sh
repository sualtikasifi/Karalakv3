#!/usr/bin/env bash
# Opens the debug-only AccountPreviewActivity and holds a finger on the painted "delete my account" pill, one screenshot at rest and
# one while held, so the press effect (the pill caving into its frame) can be looked at. Run through .github/workflows/smoke-test.yml
# with script=accountpress.sh. Coordinates are for the 1080x2400 emulator (the scene is 836 x 1882 units, 1.2752 px per unit).
set -u

PKG=com.sualtikasifi.cizimhafiza
OUT=smoke-out
mkdir -p "$OUT"
APK=$(ls cizim-hafiza/app/build/outputs/apk/debug/*.apk | head -1)
adb wait-for-device
adb shell input keyevent 82 || true
adb shell settings put global hide_error_dialogs 1 || true
sleep 25
adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS || true
adb install -r -t "$APK"
adb shell cmd locale set-app-locales "$PKG" --locales tr-TR || true
adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.AccountPreviewActivity"
sleep 8
adb exec-out screencap -p > "$OUT/account_rest.png"
adb shell input swipe 536 2113 536 2113 3000 &
sleep 1.5
adb exec-out screencap -p > "$OUT/account_press_delete.png"
wait
adb logcat -d | grep -E "FATAL|AndroidRuntime" | head -40 > "$OUT/crash.txt" || true
ls -la "$OUT"
