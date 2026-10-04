#!/usr/bin/env bash
# Walks the first-launch tutorial in an emulator, step by step, with a screenshot (and a UI dump) after each:
# Opens the Levels (worlds) page in an emulator and takes screenshots: top, scrolled once, scrolled to the end.
# Run through .github/workflows/smoke-test.yml with script=worlds.sh.
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
adb shell "run-as $PKG mkdir -p shared_prefs"
printf '%s\n' "<?xml version='1.0' encoding='utf-8' standalone='yes' ?><map><boolean name=\"tutorial_completed\" value=\"true\" /><boolean name=\"feature_tour_seen\" value=\"true\" /></map>" \
  | adb shell "run-as $PKG sh -c 'cat > shared_prefs/cizim_hafiza_settings.xml'"
shot() { adb exec-out screencap -p > "$OUT/$1.png"; }
dump() { adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml "$OUT/$1.xml" >/dev/null 2>&1; }
step() { sleep "${2:-2}"; clear_overlays; shot "$1"; dump "$1"; }

# System dialogs on this slow emulator ("Pixel Launcher isn't responding") and the ads consent form can land over
# the app at any moment and swallow taps; dismiss them whenever they are on screen.
clear_overlays() {
  local xy
  for label in "Wait" "Consent"; do
    xy=$(find_xy "$label")
    if [ -n "$xy" ]; then echo "overlay: tapping '$label' at $xy"; adb shell input tap $xy; sleep 2; fi
  done
}

# Centre of the first node whose text / description contains $1 ("" when there is none).
find_xy() {
  dump _tmp
  python3 - "$1" "$OUT/_tmp.xml" <<'PY'
import sys, re, xml.etree.ElementTree as ET
label, path = sys.argv[1].lower(), sys.argv[2]
try:
    tree = ET.parse(path)
except Exception:
    sys.exit(0)
for n in tree.iter('node'):
    t = (n.get('text') or '') + ' ' + (n.get('content-desc') or '')
    if label in t.lower():
        m = re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', n.get('bounds') or '')
        if m:
            x1, y1, x2, y2 = map(int, m.groups())
            print((x1 + x2) // 2, (y1 + y2) // 2)
            break
PY
}
adb shell am start -n $ACT
sleep 8
for i in 1 2 3 4 5 6 7 8; do sleep 6; clear_overlays; done
shot 00_menu
adb shell input tap 200 1140
sleep 5
clear_overlays
shot 05_race
adb shell input tap 540 1602
sleep 4
clear_overlays
shot 08_createroom
adb shell input swipe 540 1700 540 600 400
sleep 2
shot 09_createroom_scrolled
adb logcat -d | grep -E "FATAL|AndroidRuntime|Exception" | head -40 > "$OUT/crash_createroom.txt"
