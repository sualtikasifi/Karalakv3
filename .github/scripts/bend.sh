#!/usr/bin/env bash
# Shows the gold-duel title with a curve: writes a title_nudge preference for it (debug build only) and opens the preview.
# Run through .github/workflows/smoke-test.yml with script=bend.sh.
set -u

PKG=com.sualtikasifi.cizimhafiza
OUT=smoke-out
mkdir -p "$OUT"
APK=$(ls cizim-hafiza/app/build/outputs/apk/debug/*.apk | head -1)
adb wait-for-device
adb shell input keyevent 82 || true
adb install -r -t "$APK"
adb shell am force-stop "$PKG"
adb shell "run-as $PKG sh -c 'mkdir -p shared_prefs && echo \"<?xml version=\\\"1.0\\\" encoding=\\\"utf-8\\\" standalone=\\\"yes\\\" ?><map><string name=\\\"Gold duel#oval\\\">25</string></map>\" > shared_prefs/title_nudge.xml'"
adb shell "run-as $PKG cat shared_prefs/title_nudge.xml" || true
for state in stake_picker gift_blocked; do
  adb shell am force-stop "$PKG"
  adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.ChestPreviewActivity" --es tier silver --es state "$state"
  sleep 5
  adb shell uiautomator dump /sdcard/u.xml >/dev/null 2>&1 || true
  b=$(adb shell cat /sdcard/u.xml 2>/dev/null | grep -o 'text="Wait"[^>]*bounds="\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]"' | grep -o '\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]' | head -1)
  if [ -n "$b" ]; then
    nums=$(echo "$b" | tr -c '0-9' ' ')
    set -- $nums
    adb shell input tap $(( ($1+$3)/2 )) $(( ($2+$4)/2 )); sleep 2
  fi
  adb exec-out screencap -p > "$OUT/${state}.png"
done
ls -la "$OUT"
