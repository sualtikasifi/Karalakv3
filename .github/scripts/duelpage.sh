#!/usr/bin/env bash
# The challenge page with its two painted buttons, and the window the gold button opens.
# Run through .github/workflows/smoke-test.yml with script=duelpage.sh.
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
adb shell am force-stop "$PKG"
adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.ChestPreviewActivity" --es tier silver --es state duel_create
sleep 6
shoot page
# hold the classic button down for a moment and shoot while it is down
adb shell input swipe 260 2112 260 2112 2500 &
sleep 1
adb exec-out screencap -p > "$OUT/pressed_classic.png"
wait
# hold the gold button down for a moment and shoot while it is down
adb shell input swipe 800 2112 800 2112 2500 &
sleep 1
adb exec-out screencap -p > "$OUT/pressed_gold.png"
wait
# tap it: the window (here the reason, as the preview has no signed-in player)
adb shell input tap 800 2112
sleep 3
shoot gold_window
ls -la "$OUT"
