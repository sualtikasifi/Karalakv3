#!/usr/bin/env bash
# Opens the debug-only ChestPreviewActivity in its gift states: the send window, the blocked message and the gift box.
# Run through .github/workflows/smoke-test.yml with script=gift.sh.
set -u

PKG=com.sualtikasifi.cizimhafiza
OUT=smoke-out
mkdir -p "$OUT"
APK=$(ls cizim-hafiza/app/build/outputs/apk/debug/*.apk | head -1)
adb wait-for-device
adb shell input keyevent 82 || true
adb install -r -t "$APK"
for state in gift gift_blocked gift_inbox stake_picker stake_result; do
  adb shell am force-stop "$PKG"
  adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.ChestPreviewActivity" --es tier silver --es state "$state"
  sleep 5
  # a launcher "isn't responding" box sometimes covers the window: press Wait on it first
  adb shell uiautomator dump /sdcard/u.xml >/dev/null 2>&1 || true
  b=$(adb shell cat /sdcard/u.xml 2>/dev/null | grep -o 'text="Wait"[^>]*bounds="\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]"' | grep -o '\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]' | head -1)
  if [ -n "$b" ]; then
    x1=$(echo "$b" | sed 's/\[\([0-9]*\),\([0-9]*\)\]\[\([0-9]*\),\([0-9]*\)\]/\1/'); y1=$(echo "$b" | sed 's/\[\([0-9]*\),\([0-9]*\)\]\[\([0-9]*\),\([0-9]*\)\]/\2/')
    x2=$(echo "$b" | sed 's/\[\([0-9]*\),\([0-9]*\)\]\[\([0-9]*\),\([0-9]*\)\]/\3/'); y2=$(echo "$b" | sed 's/\[\([0-9]*\),\([0-9]*\)\]\[\([0-9]*\),\([0-9]*\)\]/\4/')
    adb shell input tap $(( (x1+x2)/2 )) $(( (y1+y2)/2 )); sleep 2
  fi
  adb exec-out screencap -p > "$OUT/${state}.png"
done
ls -la "$OUT"
