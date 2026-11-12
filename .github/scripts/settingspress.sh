#!/usr/bin/env bash
# Opens Ayarlar from the home screen and holds a finger on each of the five rows in turn, one screenshot per held row (and one
# at rest), so the press effect (the face sinking into its frame) can be looked at. Run through .github/workflows/smoke-test.yml
# with script=settingspress.sh. Coordinates are for the 1080x2400 emulator.
set -u

PKG=com.sualtikasifi.cizimhafiza
ACT=$PKG/.presentation.MainActivity
OUT=smoke-out
mkdir -p "$OUT"
APK=$(ls cizim-hafiza/app/build/outputs/apk/debug/*.apk | head -1)
adb wait-for-device
adb shell input keyevent 82 || true
adb install -r -t "$APK"
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS || true
adb shell settings put global hide_error_dialogs 1 || true
adb shell "run-as $PKG mkdir -p shared_prefs"
printf '%s\n' "<?xml version='1.0' encoding='utf-8' standalone='yes' ?><map><boolean name=\"tutorial_completed\" value=\"true\" /><boolean name=\"feature_tour_seen\" value=\"true\" /></map>" \
  | adb shell "run-as $PKG sh -c 'cat > shared_prefs/cizim_hafiza_settings.xml'"
printf '%s\n' "<?xml version='1.0' encoding='utf-8' standalone='yes' ?><map><int name=\"birth_year\" value=\"1990\" /><int name=\"terms_version\" value=\"1\" /></map>" \
  | adb shell "run-as $PKG sh -c 'cat > shared_prefs/legal_consent.xml'"
shot() { adb exec-out screencap -p > "$OUT/$1.png"; }

adb shell am force-stop $PKG
adb shell am start -W -n $ACT
sleep 15
# The Ayarlar tile (art 687, 1304 of 841 x 1870).
adb shell input tap 882 1673
sleep 4
shot settings_rest
# Rows are 110 art units apart from y 1237 (x 270 keeps clear of the toggle area); 1.2834 px per unit.
for i in 0 1 2 3 4; do
  y=$(python3 -c "print(int((1237 + $i * 110) * 1.2834))")
  adb shell input swipe 540 $y 540 $y 2500 &
  sleep 1.2
  shot settings_press_$i
  wait
  sleep 0.5
done
adb logcat -d | grep -E "FATAL|AndroidRuntime" | head -40 > "$OUT/crash.txt" || true
ls -la "$OUT"
