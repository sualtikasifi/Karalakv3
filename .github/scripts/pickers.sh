#!/usr/bin/env bash
# The pen window (its title), the profile-picture window (and its scrolling), and the challenge page's two-line buttons.
# Run through .github/workflows/smoke-test.yml with script=pickers.sh.
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
open() {
  adb shell am force-stop "$PKG"
  adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.ChestPreviewActivity" --es tier silver --es state "$1"
  sleep 6
}
open pen_picker;    shoot pen_picker
open duel_create;   shoot duel_create
open avatar_picker; shoot avatar_top
adb shell dumpsys gfxinfo "$PKG" reset >/dev/null 2>&1
for i in 1 2 3 4 5 6; do adb shell input swipe 540 1700 540 700 250; sleep 0.4; done
for i in 1 2 3 4 5 6; do adb shell input swipe 540 700 540 1700 250; sleep 0.4; done
adb shell dumpsys gfxinfo "$PKG" | grep -E "Total frames rendered|Janky frames|50th|90th|95th|99th" > "$OUT/gfx.txt" || true
cat "$OUT/gfx.txt"
shoot avatar_after_scroll
ls -la "$OUT"
