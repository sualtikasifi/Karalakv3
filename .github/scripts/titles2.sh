#!/usr/bin/env bash
# Curved titles: a sign sized by its words and a two-line sign, with the tuning panel; then the panel folded and dragged.
# Run through .github/workflows/smoke-test.yml with script=titles2.sh.
set -u
PKG=com.sualtikasifi.cizimhafiza
OUT=smoke-out
mkdir -p "$OUT"
APK=$(ls cizim-hafiza/app/build/outputs/apk/debug/*.apk | head -1)
adb wait-for-device
adb shell input keyevent 82 || true
adb install -r -t "$APK"
adb shell am force-stop "$PKG"
adb shell "run-as $PKG sh -c 'mkdir -p shared_prefs && echo \"<?xml version=\\\"1.0\\\" encoding=\\\"utf-8\\\" standalone=\\\"yes\\\" ?><map><boolean name=\\\"__on\\\" value=\\\"true\\\" /><string name=\\\"Çevrimdışı Oyna#oval\\\">20</string><string name=\\\"Play Offline#oval\\\">20</string></map>\" > shared_prefs/title_nudge.xml'"
tapText() {
  adb shell uiautomator dump /sdcard/u.xml >/dev/null 2>&1 || true
  b=$(adb shell cat /sdcard/u.xml 2>/dev/null | grep -o "text=\"$1[^\"]*\"[^>]*bounds=\"\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]\"" | grep -o '\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]' | head -1)
  [ -z "$b" ] && return 1
  nums=$(echo "$b" | tr -c '0-9' ' '); set -- $nums
  CX=$(( ($1+$3)/2 )); CY=$(( ($2+$4)/2 ))
  adb shell input tap $CX $CY; sleep 2
}
findText() {
  adb shell uiautomator dump /sdcard/u.xml >/dev/null 2>&1 || true
  b=$(adb shell cat /sdcard/u.xml 2>/dev/null | grep -o "text=\"$1[^\"]*\"[^>]*bounds=\"\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]\"" | grep -o '\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]' | head -1)
  [ -z "$b" ] && return 1
  nums=$(echo "$b" | tr -c '0-9' ' '); set -- $nums
  CX=$(( ($1+$3)/2 )); CY=$(( ($2+$4)/2 ))
}
# first with the tuning mode off (only the curves), then on
adb shell "run-as $PKG sh -c 'echo \"<?xml version=\\\"1.0\\\" encoding=\\\"utf-8\\\" standalone=\\\"yes\\\" ?><map><string name=\\\"Çevrimdışı Oyna#oval\\\">20</string><string name=\\\"Play Offline#oval\\\">20</string></map>\" > shared_prefs/title_nudge.xml'"
adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.ChestPreviewActivity" --es tier silver --es state titles
sleep 6
tapText Wait || true
adb exec-out screencap -p > "$OUT/titles_modeoff.png"
adb logcat -d | grep -E "AndroidRuntime|FATAL|Exception" | tail -20 > "$OUT/log.txt" || true
adb shell am force-stop "$PKG"
adb shell "run-as $PKG sh -c 'mkdir -p shared_prefs && echo \"<?xml version=\\\"1.0\\\" encoding=\\\"utf-8\\\" standalone=\\\"yes\\\" ?><map><boolean name=\\\"__on\\\" value=\\\"true\\\" /><string name=\\\"Çevrimdışı Oyna#oval\\\">20</string><string name=\\\"Play Offline#oval\\\">20</string></map>\" > shared_prefs/title_nudge.xml'"
adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.ChestPreviewActivity" --es tier silver --es state titles
sleep 6
tapText Wait || true
adb exec-out screencap -p > "$OUT/titles_panel.png"
if findText "⠿"; then adb shell input swipe $CX $CY $CX $((CY-900)) 800; sleep 2; fi
adb exec-out screencap -p > "$OUT/titles_dragged.png"
tapText "Küçült" || tapText "K" || true
adb exec-out screencap -p > "$OUT/titles_folded.png"
ls -la "$OUT"
