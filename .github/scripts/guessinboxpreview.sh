#!/usr/bin/env bash
# Opens the debug-only GuessInboxPreviewActivity (the "they tried your drawings" window) with 1, 3 and 10 friends and takes a
# screenshot of each. Waits for the emulator's launcher to settle first (otherwise its "isn't responding" box covers the shot).
# Run through .github/workflows/smoke-test.yml with script=guessinboxpreview.sh.
set -u

PKG=com.sualtikasifi.cizimhafiza
OUT=smoke-out
mkdir -p "$OUT"
APK=$(ls cizim-hafiza/app/build/outputs/apk/debug/*.apk | head -1)
adb wait-for-device
adb shell input keyevent 82 || true
adb shell settings put global hide_error_dialogs 1 || true
sleep 25
adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS || true
adb install -r -t "$APK"
adb shell cmd locale set-app-locales "$PKG" --locales tr-TR || true
for n in 1 3 10; do
  adb shell am force-stop "$PKG"
  adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.GuessInboxPreviewActivity" --ei n "$n"
  sleep 6
  adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS || true
  sleep 1
  adb exec-out screencap -p > "$OUT/guessinbox_$n.png"
done
for step in Ask Started; do
  adb shell am force-stop "$PKG"
  adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.XpOfferPreviewActivity" --es step "$step"
  sleep 6
  adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS || true
  sleep 1
  adb exec-out screencap -p > "$OUT/xpoffer_$step.png"
done
adb logcat -d | grep -E "FATAL|AndroidRuntime" | head -40 > "$OUT/crash.txt" || true
ls -la "$OUT"
