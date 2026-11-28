#!/usr/bin/env bash
# Title-tuning arrows over a dialog, and a button caving in under a finger.
# Writes the title_nudge preference (debug build only), opens the gold-duel preview, shoots it at rest and while the
# "play without gold" button is held down. Run through .github/workflows/smoke-test.yml with script=nudgeover.sh.
set -u

PKG=com.sualtikasifi.cizimhafiza
OUT=smoke-out
mkdir -p "$OUT"
APK=$(ls cizim-hafiza/app/build/outputs/apk/debug/*.apk | head -1)
adb wait-for-device
adb shell input keyevent 82 || true
adb install -r -t "$APK"
adb shell am force-stop "$PKG"
adb shell "run-as $PKG sh -c 'mkdir -p shared_prefs && echo \"<?xml version=\\\"1.0\\\" encoding=\\\"utf-8\\\" standalone=\\\"yes\\\" ?><map><boolean name=\\\"__on\\\" value=\\\"true\\\" /></map>\" > shared_prefs/title_nudge.xml'"
adb shell am force-stop "$PKG"
adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.ChestPreviewActivity" --es tier silver --es state stake_picker
sleep 6
adb shell uiautomator dump /sdcard/u.xml >/dev/null 2>&1 || true
b=$(adb shell cat /sdcard/u.xml 2>/dev/null | grep -o 'text="Wait"[^>]*bounds="\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]"' | grep -o '\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]' | head -1)
if [ -n "$b" ]; then
  nums=$(echo "$b" | tr -c '0-9' ' ')
  set -- $nums
  adb shell input tap $(( ($1+$3)/2 )) $(( ($2+$4)/2 )); sleep 2
fi
adb exec-out screencap -p > "$OUT/rest.png"
# hold the orange button (about 0.78 of the way down, centred) for 2 s and shoot while it is down
adb shell input swipe 540 1740 540 1740 2500 &
sleep 1
adb exec-out screencap -p > "$OUT/pressed.png"
wait
ls -la "$OUT"
