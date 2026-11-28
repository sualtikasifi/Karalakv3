#!/usr/bin/env bash
# The drawing screen on the new scene picture: at rest, and each painted button held down.
set -u
PKG=com.sualtikasifi.cizimhafiza
OUT=smoke-out
mkdir -p "$OUT"
APK=$(ls cizim-hafiza/app/build/outputs/apk/debug/*.apk | head -1)
adb wait-for-device
adb shell input keyevent 82 || true
adb install -r -t "$APK"
adb shell cmd locale set-app-locales "$PKG" --locales tr >/dev/null 2>&1 || true
adb shell am force-stop "$PKG"
adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.ChestPreviewActivity" --es tier silver --es state draw
sleep 7
adb shell input tap 540 1200 >/dev/null 2>&1; sleep 1
adb exec-out screencap -p > "$OUT/draw_rest.png"
hold() { # name x y
  adb shell input swipe $2 $3 $2 $3 2500 &
  sleep 1
  adb exec-out screencap -p > "$OUT/draw_$1.png"
  wait
}
hold pen 122 1899
hold eraser 280 1899
hold trash 595 1899
hold mute 815 170
hold back 100 205
ls -la "$OUT"
