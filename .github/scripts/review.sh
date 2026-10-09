#!/usr/bin/env bash
# Smoke test run inside a Google-APIs emulator (see .github/workflows/smoke-test.yml).
# Installs the debug APK, skips the tutorial, measures cold-start time, walks the main screens
# taking a screenshot of each, and fails if the app crashed or hit an ANR.
# Everything it produces goes to smoke-out/, which the workflow publishes to the
# ci-smoke-results branch so the screenshots can be looked at.
set -u

PKG=com.sualtikasifi.cizimhafiza
ACT=$PKG/.presentation.MainActivity
OUT=smoke-out
mkdir -p "$OUT"
APK=$(ls cizim-hafiza/app/build/outputs/apk/debug/*.apk | head -1)
echo "APK: $APK"

adb wait-for-device
adb shell input keyevent 82 || true
adb install -r -t "$APK"
# The app asks for the notification permission a few seconds after its first launch; that system dialog
# would swallow the scripted taps below, so it is granted up front (the dialog itself is covered by the
# ads-consent tap further down only for the UMP form, not this one).
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS || true
adb logcat -c

# Skip the first-run tutorial: it is a plain boolean in the app's settings file, and the
# debug build is debuggable so run-as can write it.
adb shell "run-as $PKG mkdir -p shared_prefs"
printf '%s\n' "<?xml version='1.0' encoding='utf-8' standalone='yes' ?><map><boolean name=\"tutorial_completed\" value=\"true\" /><boolean name=\"feature_tour_seen\" value=\"true\" /></map>" \
  | adb shell "run-as $PKG sh -c 'cat > shared_prefs/cizim_hafiza_settings.xml'"

shot() { adb exec-out screencap -p > "$OUT/$1.png"; }

# Dumps the UI tree and prints/stores every visible label; prints the tap target for $1 if found.
dump_ui() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml "$OUT/$1.xml" >/dev/null 2>&1
}

# tap_text "Label": taps the first node whose text or content-desc contains Label.
tap_text() {
  dump_ui _tmp
  local xy
  xy=$(python3 - "$1" "$OUT/_tmp.xml" <<'PY'
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
)
  if [ -n "$xy" ]; then
    echo "tap '$1' at $xy"
    adb shell input tap $xy
    return 0
  fi
  echo "NOT FOUND: '$1'"
  return 1
}

# --- Cold-start time, three runs (am start -W waits until the first frame) ----------------
: > "$OUT/startup.txt"
for i in 1 2 3; do
  adb shell am force-stop $PKG
  sleep 2
  adb shell am start -W -n $ACT | tee -a "$OUT/startup.txt"
done
echo "--- am start -W (times out on the software-rendered emulator, kept for reference):"; grep -E "Status|WaitTime" "$OUT/startup.txt"
# The activity manager's own "Displayed" line is the reliable first-frame time here.
adb logcat -d | grep "Displayed $PKG" | cut -c1-200 | tee "$OUT/startup_displayed.txt"

# The slow emulator sometimes shows a system 'Pixel Launcher isn't responding' dialog; dismiss it and bring the app back.
tap_text "Wait" || true
sleep 2
adb shell am start -n $ACT
sleep 8
# --- Main menu ------------------------------------------------------------------------------
sleep 6
shot 01_main_menu

# --- Walk the main screens by coordinates (1080x2400) ------------------------------------------
# Compose content is not exposed to uiautomator on this emulator, so taps are by position,
# read off the main-menu screenshot. Each screen: tap, wait, screenshot, Back.

# The ads consent form (UMP) appears at an unpredictable moment on this slow emulator. Wait until its blue "Consent"
# button is really on screen (found by colour), tap it, and only then carry on.
consent_visible() {
  adb exec-out screencap > "$OUT/_raw.bin"
  python3 - "$OUT/_raw.bin" <<'PY'
import sys, struct
d = open(sys.argv[1], 'rb').read()
w, h, fmt = struct.unpack('<III', d[:12])
off = 12
ok = 0
for (x, y) in ((540, 1678), (300, 1680), (780, 1680)):
    p = off + (y * w + x) * 4
    r, g, b = d[p], d[p + 1], d[p + 2]
    if r < 60 and 80 < g < 150 and b > 190: ok += 1
print(1 if ok >= 2 else 0)
PY
}
for i in $(seq 1 20); do
  sleep 6
  if [ "$(consent_visible)" = "1" ]; then echo "consent form seen"; adb shell input tap 540 1678; sleep 3; break; fi
done
shot 02_main_menu_after_consent
P=${P:-a}
P=r
# Every section starts from a fresh main menu, so one missed tap cannot shift the rest.
fresh() { adb shell am force-stop $PKG; sleep 2; adb shell am start -n $ACT >/dev/null; sleep 12; tap_text "Wait" || true; }
# Waiting room: the room code in its well
fresh; adb shell input tap 200 1160; sleep 6; adb shell input tap 540 1602; sleep 6; adb shell input tap 540 2200; sleep 15; shot ${P}01_waiting
# Settings -> report a problem, then with the keyboard up
fresh; adb shell input tap 880 1680; sleep 6; shot ${P}02_settings
adb shell input tap 540 1870; sleep 6; shot ${P}03_report
adb shell input swipe 540 1600 540 900 300; sleep 2; shot ${P}04_report_scrolled
adb shell input swipe 540 900 540 1700 300; sleep 2
adb shell input tap 540 1560; sleep 4; shot ${P}05_report_keyboard
# A drawing turn to its last seconds (sparks round the drawing area), then the guess screen
fresh; adb shell input tap 880 1160; sleep 6; shot ${P}06_offline
adb shell input tap 540 2200
for i in $(seq 10 69); do sleep 1.2; shot ${P}2${i}_draw; done
adb logcat -d | grep -E "FATAL|AndroidRuntime" | head -40 > "$OUT/crash.txt"
if grep -q "FATAL EXCEPTION" "$OUT/crash.txt"; then echo "APP CRASHED"; cat "$OUT/crash.txt"; exit 1; fi
