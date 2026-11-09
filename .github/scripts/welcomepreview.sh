#!/usr/bin/env bash
# The tutorial's welcome screen on a brand-new install, in Turkish: at rest, and with a finger held on "Hadi başlayalım"
# (the whole button, leaves and all, should cave in). Run through .github/workflows/smoke-test.yml with script=welcomepreview.sh.
set -u

PKG=com.sualtikasifi.cizimhafiza
ACT=$PKG/.presentation.MainActivity
OUT=smoke-out
mkdir -p "$OUT"
APK=$(ls cizim-hafiza/app/build/outputs/apk/debug/*.apk | head -1)
adb wait-for-device
adb shell input keyevent 82 || true
adb shell settings put global hide_error_dialogs 1 || true
sleep 20
adb install -r -t "$APK"
adb shell pm clear $PKG >/dev/null 2>&1 || true
adb shell cmd locale set-app-locales "$PKG" --locales tr-TR || true
shot() { adb exec-out screencap -p > "$OUT/welcome_$1.png"; }
adb shell am start -n $ACT >/dev/null
sleep 12
shot rest
# a finger held on the button's middle (about 541, 1591 on a 1080x2400 screen); the picture is taken while it is down
adb shell input swipe 541 1591 541 1591 2600 &
sleep 0.9
shot pressed
wait
shot after
