#!/usr/bin/env bash
# The notice cards of the gold-duel and gift windows, and a button caving in under a finger (no tuning panel in the way).
# Run through .github/workflows/smoke-test.yml with script=notice.sh.
set -u

PKG=com.sualtikasifi.cizimhafiza
OUT=smoke-out
mkdir -p "$OUT"
APK=$(ls cizim-hafiza/app/build/outputs/apk/debug/*.apk | head -1)
adb wait-for-device
adb shell input keyevent 82 || true
adb install -r -t "$APK"
adb shell "run-as $PKG rm -f shared_prefs/title_nudge.xml" || true
shoot() {
  local name="$1"
  adb shell uiautomator dump /sdcard/u.xml >/dev/null 2>&1 || true
  b=$(adb shell cat /sdcard/u.xml 2>/dev/null | grep -o 'text="Wait"[^>]*bounds="\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]"' | grep -o '\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]' | head -1)
  if [ -n "$b" ]; then
    nums=$(echo "$b" | tr -c '0-9' ' ')
    set -- $nums
    adb shell input tap $(( ($1+$3)/2 )) $(( ($2+$4)/2 )); sleep 2
  fi
  adb exec-out screencap -p > "$OUT/$name.png"
}
for state in stake_blocked gift_blocked2 stake_picker; do
  adb shell am force-stop "$PKG"
  adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.ChestPreviewActivity" --es tier silver --es state "$state"
  sleep 6
  shoot "$state"
done
# hold the first row of the picker down for 2.5 s and shoot while it is down
adb shell input swipe 540 1220 540 1220 2500 &
sleep 1
adb exec-out screencap -p > "$OUT/pressed_row.png"
wait
adb shell am force-stop "$PKG"
adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.ChestPreviewActivity" --es tier silver --es state stake_picker
sleep 6
adb shell input swipe 540 1740 540 1740 2500 &
sleep 1
adb exec-out screencap -p > "$OUT/pressed_button.png"
wait
ls -la "$OUT"
