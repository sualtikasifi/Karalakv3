#!/usr/bin/env bash
# Opens the debug-only GuessNoticePreviewActivity (the Guess It notice page) once per case and takes a screenshot of each.
# Run through .github/workflows/smoke-test.yml with script=guessnoticepreview.sh.
set -u

PKG=com.sualtikasifi.cizimhafiza
OUT=smoke-out
mkdir -p "$OUT"
APK=$(ls cizim-hafiza/app/build/outputs/apk/debug/*.apk | head -1)
adb wait-for-device
adb shell input keyevent 82 || true
adb install -r -t "$APK"
adb shell settings put global hide_error_dialogs 1 || true
for problem in Own NotFound Offline Child; do
  adb shell am force-stop "$PKG"
  adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.GuessNoticePreviewActivity" --es problem "$problem"
  sleep 3
  adb exec-out screencap -p > "$OUT/guessnotice_${problem}.png"
done
for case in "0 -" "1 Kedi" "2 Kedi"; do
  set -- $case
  adb shell am force-stop "$PKG"
  extra=""
  [ "$2" != "-" ] && extra="--es typed $2"
  adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.GuessAnswerPreviewActivity" --ei attempts "$1" $extra
  sleep 3
  adb exec-out screencap -p > "$OUT/guessanswer_$1.png"
done
adb logcat -d | grep -E "FATAL|AndroidRuntime" | head -40 > "$OUT/crash.txt" || true
ls -la "$OUT"
