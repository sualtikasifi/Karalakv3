#!/usr/bin/env bash
# Opens each page whose title is lettered on a painted sign (debug-only TitlesPreviewActivity) and takes a screenshot.
# Run through .github/workflows/smoke-test.yml with script=titles_preview.sh.
set -u

PKG=com.sualtikasifi.cizimhafiza
OUT=smoke-out
mkdir -p "$OUT"
APK=$(ls cizim-hafiza/app/build/outputs/apk/debug/*.apk | head -1)
adb wait-for-device
adb shell input keyevent 82 || true
adb shell settings put global hide_error_dialogs 1 || true
sleep 20
adb install -r -t "$APK"
# the game is played in Turkish: show its string resources in Turkish (per-app language, Android 13+)
adb shell cmd locale set-app-locales "$PKG" --locales tr-TR || true
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS || true
adb shell "run-as $PKG mkdir -p shared_prefs"
printf '%s\n' "<?xml version='1.0' encoding='utf-8' standalone='yes' ?><map><boolean name=\"tutorial_completed\" value=\"true\" /><boolean name=\"feature_tour_seen\" value=\"true\" /></map>" \
  | adb shell "run-as $PKG sh -c 'cat > shared_prefs/cizim_hafiza_settings.xml'"
for s in settings store achievements league join lobby report worlds wordcount searching found; do
  adb shell am force-stop $PKG
  adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.TitlesPreviewActivity" --es screen $s
  sleep 9
  adb exec-out screencap -p > "$OUT/title_$s.png"
done
adb logcat -d | grep -E "FATAL|AndroidRuntime" | head -20 > "$OUT/crash.txt" || true
ls -la "$OUT"
