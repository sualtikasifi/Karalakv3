#!/usr/bin/env bash
# One emulator session, three checks: the friends page with the "how do you want to play" window, the challenge page (normal and very long
# name, for the longer explainer), and the "x2 XP" button. Run through .github/workflows/smoke-test.yml with script=finalpreview.sh.
set -u

PKG=com.sualtikasifi.cizimhafiza
DBG=$PKG.debug
OUT=smoke-out
mkdir -p "$OUT"
APK=$(ls cizim-hafiza/app/build/outputs/apk/debug/*.apk | head -1)
adb wait-for-device
adb shell input keyevent 82 || true
adb shell settings put global hide_error_dialogs 1 || true
sleep 20
adb install -r -t "$APK"

# 1. friends page, then the first friend's "Challenge" button (1080x2400 emulator: the button sits at about 714,1622)
adb shell am force-stop "$PKG"
adb shell am start -W -n "$PKG/$DBG.FriendsPreviewActivity"
sleep 8
adb exec-out screencap -p > "$OUT/fin_friends_1.png"
adb shell input tap 714 1622
sleep 2
adb exec-out screencap -p > "$OUT/fin_friends_2.png"

# 2. the challenge page
for name in "Bob Ross" "Maximilian Alexander von Hohenzollern"; do
  tag=$(echo "$name" | tr -cd 'A-Za-z' | cut -c1-8)
  adb shell am force-stop "$PKG"
  adb shell am start -W -n "$PKG/$DBG.DuelCreatePreviewActivity" --es name "$name"
  sleep 4
  adb exec-out screencap -p > "$OUT/fin_create_${tag}.png"
done

# 3. the x2 XP button
for xp in 171 5230; do
  adb shell am force-stop "$PKG"
  adb shell am start -W -n "$PKG/$DBG.DoubleXpPreviewActivity" --ei xp "$xp"
  sleep 4
  adb exec-out screencap -p > "$OUT/fin_x2_$xp.png"
done

adb logcat -d | grep -E "FATAL|AndroidRuntime" | head -40 > "$OUT/crash.txt" || true
ls -la "$OUT"
