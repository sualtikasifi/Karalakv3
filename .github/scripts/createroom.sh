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
adb shell settings put global hide_error_dialogs 1 || true
adb shell "run-as $PKG mkdir -p shared_prefs"
printf '%s\n' "<?xml version='1.0' encoding='utf-8' standalone='yes' ?><map><boolean name=\"tutorial_completed\" value=\"true\" /><boolean name=\"feature_tour_seen\" value=\"true\" /></map>" \
  | adb shell "run-as $PKG sh -c 'cat > shared_prefs/cizim_hafiza_settings.xml'"
# The first-run year-of-birth and terms prompt (LegalConsentHost) would cover every screen: answered up front.
printf '%s\n' "<?xml version='1.0' encoding='utf-8' standalone='yes' ?><map><int name=\"birth_year\" value=\"1990\" /><int name=\"terms_version\" value=\"1\" /></map>" \
  | adb shell "run-as $PKG sh -c 'cat > shared_prefs/legal_consent.xml'"
shot() { adb exec-out screencap -p > "$OUT/$1.png"; }
dump() { adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml "$OUT/$1.xml" >/dev/null 2>&1; }
step() { sleep "${2:-2}"; clear_overlays; shot "$1"; dump "$1"; }

# System dialogs on this slow emulator ("Pixel Launcher isn't responding") and the ads consent form can land over
# the app at any moment and swallow taps; dismiss them whenever they are on screen.
clear_overlays() {
  local xy
  for label in "Wait" "Consent"; do
    xy=$(find_exact "$label")
    if [ -n "$xy" ]; then echo "overlay: tapping '$label' at $xy"; adb shell input tap $xy; sleep 2; fi
  done
}

# Centre of the first node whose text / description is exactly $1 ("" when there is none).
find_exact() {
  dump _tmp
  python3 - "$1" "$OUT/_tmp.xml" <<'PY'
import sys, re, xml.etree.ElementTree as ET
label, path = sys.argv[1].lower(), sys.argv[2]
try:
    tree = ET.parse(path)
except Exception:
    sys.exit(0)
for n in tree.iter('node'):
    t = (n.get('text') or '').strip().lower()
    d = (n.get('content-desc') or '').strip().lower()
    if label in (t, d):
        m = re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', n.get('bounds') or '')
        if m:
            x1, y1, x2, y2 = map(int, m.groups())
            print((x1 + x2) // 2, (y1 + y2) // 2)
            break
PY
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
sleep 50
# Accepts the ads consent form only if it is really on screen (its blue button sampled at three points):
# a blind tap where the button would be lands on the Store tile when no form shows.
consent_tap() {
  adb exec-out screencap > /tmp/_consent.bin
  if [ "$(python3 - /tmp/_consent.bin <<'PY'
import sys, struct
d = open(sys.argv[1], 'rb').read()
w, h, fmt = struct.unpack('<III', d[:12])
ok = 0
for (x, y) in ((540, 1678), (300, 1680), (780, 1680)):
    p = 12 + (y * w + x) * 4
    r, g, b = d[p], d[p + 1], d[p + 2]
    if r < 60 and 80 < g < 150 and b > 190: ok += 1
print(1 if ok >= 2 else 0)
PY
)" = "1" ]; then adb shell input tap 540 1678; fi
}
consent_tap
sleep 4
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
# Create the room, then send a reaction and a phrase from the chat bar so the own player card shows a message.
for label in "Oda Oluştur" "Create Room" "Create"; do
  xy=$(find_xy "$label"); [ -n "$xy" ] && { adb shell input tap $xy; break; }
done
sleep 18
clear_overlays
shot 10_lobby
adb shell input tap 528 2187
sleep 1
shot 11_lobby_emoji
adb shell input tap 438 2187
sleep 3
shot 12_lobby_picker
adb shell input tap 112 1880
sleep 1
shot 13_lobby_message
adb logcat -d | grep -E "FATAL|AndroidRuntime|Exception" | head -40 > "$OUT/crash_createroom.txt"
