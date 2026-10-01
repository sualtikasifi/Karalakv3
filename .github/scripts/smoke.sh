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

# --- Main menu ------------------------------------------------------------------------------
sleep 6
shot 01_main_menu

# --- Walk the main screens by coordinates (1080x2400) ------------------------------------------
# Compose content is not exposed to uiautomator on this emulator, so taps are by position,
# read off the main-menu screenshot. Each screen: tap, wait, screenshot, Back.

# The ads consent form (UMP) opens over the main menu on a fresh install; accept it so the
# buttons underneath can be reached (the emulator counts as an EEA device).
adb shell input tap 540 1678
sleep 3
shot 02_main_menu_after_consent

tour() {  # tour <file-name> <x> <y>
  adb shell input tap "$2" "$3"
  sleep 4
  shot "$1"
  adb shell input keyevent KEYCODE_BACK
  sleep 2
}
tour 03_levels        200 1488
tour 04_friends       540 1488
tour 05_achievements  880 1488
tour 06_league        200 1692
tour 07_store         540 1692
tour 08_settings      880 1692
tour 09_daily         540  756
tour 10_offline       880 1146
# League, Global tab (table, compact prize banner, monthly XP card).
adb shell input tap 200 1692
sleep 4
adb shell input tap 794 386
sleep 6
shot 06b_league_global
adb shell input keyevent KEYCODE_BACK
sleep 2

# Settings > Account (profile header, stats, username, backup), top and bottom of the page.
adb shell input tap 880 1692
sleep 4
adb shell input tap 540 2174
sleep 5
shot 08b_account
adb shell input swipe 540 1800 540 500 400
sleep 2
shot 08c_account_bottom
adb shell input keyevent KEYCODE_BACK
sleep 2
adb shell input keyevent KEYCODE_BACK
sleep 2

shot 11_back_on_main_menu
walk() { :; }

# --- Play one whole round hands-off: Daily Challenge, draw -> break -> guess -> result --------------
# The word is unknown to the script, so the guessing phase is left to run out its timers; what is
# checked is that every phase appears, moves on by itself and ends on the result screen without a
# crash. A few strokes are drawn during the first drawing turns so the canvas is exercised.
adb shell input tap 540 756          # Daily Challenge card on the main menu
sleep 5
for i in $(seq 1 16); do
  if [ "$i" -le 6 ]; then
    adb shell input swipe 200 900 880 1100 300
    adb shell input swipe 880 1300 200 1600 300
  fi
  adb exec-out screencap -p > "$OUT/flow_$(printf %02d "$i").png"
  sleep 8
done

# Flow screenshots are many, so keep them small (half size JPEG) before publishing.
python3 -m pip install -q pillow >/dev/null 2>&1 && python3 - "$OUT" <<'PY'
import sys, glob, os
from PIL import Image
for f in sorted(glob.glob(os.path.join(sys.argv[1], 'flow_*.png'))):
    im = Image.open(f).convert('RGB')
    im = im.resize((im.width // 2, im.height // 2))
    im.save(f[:-4] + '.jpg', quality=60)
    os.remove(f)
PY

# --- Verdict ----------------------------------------------------------------------------------
adb logcat -d > "$OUT/logcat.txt"
adb logcat -d -b crash > "$OUT/crash.txt"
FAIL=0
# "FATAL EXCEPTION" lines also come from tools run on the emulator (uiautomator, am), so
# only a crash whose "Process:" line names this app counts.
if grep -A 3 "FATAL EXCEPTION" "$OUT/logcat.txt" | grep -q "Process: $PKG"; then
  echo "::error::The app crashed (FATAL EXCEPTION in $PKG)"
  grep -A 14 "FATAL EXCEPTION" "$OUT/logcat.txt" | head -60
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
