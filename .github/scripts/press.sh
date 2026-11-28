#!/usr/bin/env bash
# The gold-duel and gift windows at rest and with a row / the start button held down.
# Run through .github/workflows/smoke-test.yml with script=press.sh.
set -u
PKG=com.sualtikasifi.cizimhafiza
OUT=smoke-out
mkdir -p "$OUT"
APK=$(ls cizim-hafiza/app/build/outputs/apk/debug/*.apk | head -1)
adb wait-for-device
adb shell input keyevent 82 || true
adb install -r -t "$APK"
adb shell "run-as $PKG rm -f shared_prefs/title_nudge.xml" || true
dismiss_anr() {
  adb shell uiautomator dump /sdcard/u.xml >/dev/null 2>&1 || true
  b=$(adb shell cat /sdcard/u.xml 2>/dev/null | grep -o 'text="Wait"[^>]*bounds="\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]"' | grep -o '\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]' | head -1)
  if [ -n "$b" ]; then
    nums=$(echo "$b" | tr -c '0-9' ' ')
    set -- $nums
    adb shell input tap $(( ($1+$3)/2 )) $(( ($2+$4)/2 )); sleep 2
  fi
}
open() {
  adb shell am force-stop "$PKG"
  adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.ChestPreviewActivity" --es tier silver --es state "$1"
  sleep 6; dismiss_anr
}
hold() { # x y name
  dismiss_anr
  adb shell input swipe "$1" "$2" "$1" "$2" 2500 &
  sleep 1
  adb exec-out screencap -p > "$OUT/$3.png"
  wait; sleep 1
}
open stake_picker
adb exec-out screencap -p > "$OUT/stake_rest.png"
hold 540 1352 stake_row2_pressed
adb exec-out screencap -p > "$OUT/stake_row2_selected.png"
hold 540 1755 stake_start_pressed
open gift
adb exec-out screencap -p > "$OUT/gift_rest.png"
hold 540 1155 gift_row2_pressed
ls -la "$OUT"
