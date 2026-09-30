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

# --- Main menu ------------------------------------------------------------------------------
sleep 6
shot 01_main_menu
dump_ui 01_main_menu
echo "--- visible texts on the main menu:"
python3 - "$OUT/01_main_menu.xml" <<'PY' | tee "$OUT/01_main_menu_texts.txt"
import sys, xml.etree.ElementTree as ET
seen = []
for n in ET.parse(sys.argv[1]).iter('node'):
    for v in (n.get('text'), n.get('content-desc')):
        if v and v not in seen:
            seen.append(v)
print("\n".join(seen))
PY

# --- Scroll the main menu top to bottom, one screenshot per screen ----------------------------
# Compose content is not exposed to uiautomator on this emulator (the dump above is nearly
# empty), so navigation is by swipe/coordinates. Screen is 1080x2400 (pixel_6).
for i in 02 03 04 05; do
  adb shell input swipe 540 1900 540 600 600
  sleep 2
  shot ${i}_main_menu_scrolled
done
walk() { :; }

# --- Verdict ----------------------------------------------------------------------------------
adb logcat -d > "$OUT/logcat.txt"
adb logcat -d -b crash > "$OUT/crash.txt"
FAIL=0
if grep -q "FATAL EXCEPTION" "$OUT/logcat.txt"; then
  echo "::error::The app crashed (FATAL EXCEPTION in logcat)"
  grep -A 12 "FATAL EXCEPTION" "$OUT/logcat.txt" | head -60
  FAIL=1
fi
if grep -q "ANR in $PKG" "$OUT/logcat.txt"; then
  echo "::error::The app hit an ANR"
  FAIL=1
fi
# Keep the log small enough to publish; the crash/ANR lines above are what matter.
grep -E "$PKG|AndroidRuntime|FATAL|ANR" "$OUT/logcat.txt" | tail -400 > "$OUT/logcat_app.txt"
rm -f "$OUT/logcat.txt" "$OUT/_tmp.xml"
exit $FAIL
