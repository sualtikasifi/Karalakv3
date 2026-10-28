#!/usr/bin/env bash
# The first-run legal prompt (LegalConsentHost) and the under-13 mode, in an emulator. Two installs:
#  1) an adult year: the prompt appears after the tutorial, takes 1990, closes, and the home screen works;
#  2) a child's year: the prompt takes 2016, and Quick Match shows the "for ages 13 and up" panel instead.
# Run through .github/workflows/smoke-test.yml with script=legal.sh; results go to smoke-out/ like smoke.sh's.
set -u

PKG=com.sualtikasifi.cizimhafiza
ACT=$PKG/.presentation.MainActivity
OUT=smoke-out
mkdir -p "$OUT"
APK=$(ls cizim-hafiza/app/build/outputs/apk/debug/*.apk | head -1)

adb wait-for-device
adb shell input keyevent 82 || true

shot() { adb exec-out screencap -p > "$OUT/$1.png"; }
dump_ui() { adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml "$OUT/$1.xml" >/dev/null 2>&1; }
# Prints "x y" of the first node whose text or content-desc contains $1 (case-insensitive).
find_xy() {
  dump_ui _tmp
  python3 - "$1" "$OUT/_tmp.xml" <<'PY'
import sys, re, xml.etree.ElementTree as ET
label, path = sys.argv[1].lower(), sys.argv[2]
try:
    tree = ET.parse(path)
except Exception:
    sys.exit(0)
for n in tree.iter('node'):
    t = ((n.get('text') or '') + ' ' + (n.get('content-desc') or '') + ' ' + (n.get('hint') or '')).lower()
    if label in t:
        m = re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', n.get('bounds') or '')
        if m:
            x1, y1, x2, y2 = map(int, m.groups())
            print((x1 + x2) // 2, (y1 + y2) // 2)
            break
PY
}
tap_label() { local xy; xy=$(find_xy "$1"); [ -n "$xy" ] && { echo "tap '$1' at $xy"; adb shell input tap $xy; return 0; }; echo "NOT FOUND: '$1'"; return 1; }

run_case() {  # run_case <name> <year>
  adb uninstall $PKG >/dev/null 2>&1 || true
  adb install -r -t "$APK"
  adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS || true
  # The tutorial is already behind this player; the legal prompt (not seeded) is what is being tested.
  adb shell "run-as $PKG mkdir -p shared_prefs"
  printf '%s\n' "<?xml version='1.0' encoding='utf-8' standalone='yes' ?><map><boolean name=\"tutorial_completed\" value=\"true\" /><boolean name=\"feature_tour_seen\" value=\"true\" /></map>" \
    | adb shell "run-as $PKG sh -c 'cat > shared_prefs/cizim_hafiza_settings.xml'"
  adb logcat -c
  adb shell am start -n $ACT
  sleep 14
  shot "$1_01_prompt"
  # The year field (its placeholder reads YYYY/AAAA), then the year, then Continue.
  tap_label "YYYY" || tap_label "AAAA" || adb shell input tap 540 1150
  sleep 1
  adb shell input text "$2"
  sleep 1
  adb shell input keyevent KEYCODE_BACK   # closes the keyboard, not the dialog (it ignores Back)
  sleep 1
  shot "$1_02_year_typed"
  tap_label "Continue" || tap_label "Devam"
  sleep 4
  shot "$1_03_after_prompt"
  # Consent form (EEA in debug builds), only by its label.
  tap_label "Consent" || true
  sleep 3
  # Quick Match tile (main menu, 1080x2400)
  adb shell input tap 540 1170
  sleep 5
  shot "$1_04_quick_match"
  adb shell input keyevent KEYCODE_BACK
  sleep 2
  adb logcat -d > "$OUT/$1_logcat.txt"
}

run_case adult 1990
run_case child 2016

FAIL=0
for f in "$OUT"/adult_logcat.txt "$OUT"/child_logcat.txt; do
  if grep -A 3 "FATAL EXCEPTION" "$f" | grep -q "Process: $PKG"; then
    echo "::error::The app crashed ($f)"; grep -A 14 "FATAL EXCEPTION" "$f" | head -40; FAIL=1
  fi
  grep -E "$PKG|AndroidRuntime|FATAL" "$f" | tail -200 > "${f%.txt}_app.txt"; rm -f "$f"
done
rm -f "$OUT/_tmp.xml"
exit $FAIL
