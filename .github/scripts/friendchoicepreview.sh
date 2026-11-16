#!/usr/bin/env bash
# Friends page (debug-only FriendsPreviewActivity): the list with its "Meydan Oku" buttons, then the "how do you want to play" window
# opened from the first row. Run through .github/workflows/smoke-test.yml with script=friendchoicepreview.sh.
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
adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.FriendsPreviewActivity"
sleep 8
adb exec-out screencap -p > "$OUT/friend_choice_1.png"
# the first friend's button (1080x2400 emulator)
adb shell input tap 700 1500
sleep 2
adb exec-out screencap -p > "$OUT/friend_choice_2.png"
adb logcat -d | grep -E "FATAL|AndroidRuntime" | head -40 > "$OUT/crash.txt" || true
ls -la "$OUT"
