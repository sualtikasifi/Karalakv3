#!/usr/bin/env bash
# Draws with each premium pen in a solo game and screenshots the stroke (twice, a moment apart, to catch the motion).
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

for PEN in CLASSIC SPECTER COSMOS SPECTER; do
  echo "=== $PEN ==="
  adb shell am force-stop $PKG
  sleep 2
  printf '%s\n' "<?xml version='1.0' encoding='utf-8' standalone='yes' ?><map><boolean name=\"tutorial_completed\" value=\"true\" /><boolean name=\"feature_tour_seen\" value=\"true\" /><string name=\"selected_pen_skin\">$PEN</string></map>" \
    | adb shell "run-as $PKG sh -c 'cat > shared_prefs/cizim_hafiza_settings.xml'"
  adb shell am start -n $ACT
  sleep 14
  for i in $(seq 1 6); do
    if [ "$(consent_visible)" = "1" ]; then echo "consent form seen"; adb shell input tap 540 1678; sleep 3; break; fi
    sleep 4
  done
  tap_text "Wait" || true
  # The daily-streak window opens over the home screen on a fresh start: close it, then answer a test ad if one shows.
  adb shell input tap 980 752; sleep 2
  for i in 1 2 3; do sleep 4; tap_text "Close" || tap_text "Skip" || true; done
  shot ${PEN}_00_home
  adb shell input tap 880 1140
  sleep 5
  adb shell input tap 540 2200
  sleep 9
  sleep 3
  shot ${PEN}_01_game
  sleep 2
  adb shell input swipe 200 800 800 1000 700
  adb shell input swipe 800 1000 300 1250 700
  adb shell input swipe 300 1250 850 1500 700
  shot ${PEN}_02_drawn_a
  sleep 1
  shot ${PEN}_03_drawn_b
done
adb logcat -d | grep -E "FATAL EXCEPTION|ANR in" | head -5 | tee "$OUT/crashes.txt"
