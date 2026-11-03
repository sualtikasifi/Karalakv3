#!/usr/bin/env bash
# Seeds a small "Gerçek Olsaydı" project on the debug build, then opens the studio's wizard on each of its seven steps
# (and the voices' five, the library and the settings) and takes a screenshot of each, plain and scrolled down.
# Run through .github/workflows/smoke-test.yml with script=wizardpreview.sh.
set -u

PKG=com.sualtikasifi.cizimhafiza
ACT="$PKG/com.sualtikasifi.cizimhafiza.debug.DevPanelPreviewActivity"
OUT=smoke-out
mkdir -p "$OUT"
APK=$(ls cizim-hafiza/app/build/outputs/apk/debug/*.apk | head -1)
adb wait-for-device
adb shell input keyevent 82 || true
adb shell settings put global hide_error_dialogs 1 || true
sleep 20
adb install -r -t "$APK"
adb shell cmd locale set-app-locales "$PKG" --locales tr-TR || true

# a project: one drawing (a rocket) with the assistant's notes, one scene idea chosen
cat > /tmp/project.json <<'JSON'
{"drawings":[{"id":"a","strokes":[[{"x":0.4,"y":0.1},{"x":0.5,"y":0.3},{"x":0.5,"y":0.8},{"x":0.35,"y":0.8},{"x":0.35,"y":0.3},{"x":0.4,"y":0.1}],[{"x":0.36,"y":0.8},{"x":0.34,"y":0.95}],[{"x":0.49,"y":0.8},{"x":0.51,"y":0.95}]],"word":"roket","seconds":7,"nickname":"SinanK_34","level":41},
{"id":"b","strokes":[[{"x":0.2,"y":0.5},{"x":0.8,"y":0.5}]],"word":"bağlama","seconds":5,"nickname":"Zeynep","level":12}],
"reveals":[{"sampleId":"a","kind":"nesne","describe":"uzun dar gövde, tepede üçgen burun, altta üç ince çubuk ayak","material":"boyalı metal","scene":"bir balkon korkuluğunun üstü","size":"yaklaşık 40 cm","motion":"0–2 sn: el kenardan girer. 2–5 sn: parmakla gösterir. 5–8 sn: tezgâha vurur."},{"sampleId":"b"}]}
JSON
adb shell "run-as $PKG mkdir -p files/studio_real shared_prefs"
adb shell "run-as $PKG sh -c 'cat > files/studio_real/project.json'" < /tmp/project.json

prefs() {
  cat > /tmp/prefs.xml <<XML
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map><int name="studio_real_step" value="$1"/><int name="studio_real_voice_step" value="$2"/><string name="studio_real_tab">$3</string></map>
XML
  adb shell "run-as $PKG sh -c 'cat > shared_prefs/console_ai.xml'" < /tmp/prefs.xml
}

shot() {  # name step voiceStep tab
  prefs "$2" "$3" "$4"
  adb shell am force-stop "$PKG"
  adb shell am start -W -n "$ACT" --es screen Studio --es format real
  sleep 9
  adb exec-out screencap -p > "$OUT/wizard_$1.png"
  adb shell input swipe 540 1900 540 700 400
  sleep 1.5
  adb exec-out screencap -p > "$OUT/wizard_$1_down.png"
}

shot warmup 1 1 Prepare
for n in 1 2 3 4 6 7; do shot "step$n" "$n" 1 Prepare; done
for v in 1 2 3 4 5; do shot "voice$v" 5 "$v" Prepare; done
shot library 1 1 Library
shot settings 1 1 Settings

adb logcat -d -t 400 | grep -E "AndroidRuntime|FATAL" > "$OUT/crash.txt" || true
ls -la "$OUT"
