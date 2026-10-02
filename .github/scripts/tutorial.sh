#!/usr/bin/env bash
# Walks the first-launch tutorial in an emulator, step by step, with a screenshot (and a UI dump) after each:
# intro -> three drawings -> the guess turns with the two joker lessons (clock held, only the joker button works).
# Run through .github/workflows/smoke-test.yml with script=tutorial.sh; results go to smoke-out/ like smoke.sh's.
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
adb logcat -c

# Tutorial NOT completed (that is what is being tested); only the home feature tour is skipped.
adb shell "run-as $PKG mkdir -p shared_prefs"
printf '%s\n' "<?xml version='1.0' encoding='utf-8' standalone='yes' ?><map><boolean name=\"feature_tour_seen\" value=\"true\" /></map>" \
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
tap_text() {  # tap_text "Label" [fallback-x fallback-y]
  local xy; xy=$(find_xy "$1")
  if [ -z "$xy" ] && [ $# -ge 3 ]; then xy="$2 $3"; fi
  if [ -n "$xy" ]; then echo "tap '$1' at $xy"; adb shell input tap $xy; return 0; fi
  echo "NOT FOUND: '$1'"; return 1
}

# Centre of the bright-orange pulsing ring the joker lesson draws round its button, read from a raw screenshot
# (found by colour, since the Compose tree is not exposed). Prints "x y" or nothing.
ring_xy() {
  adb exec-out screencap > "$OUT/_raw.bin"
  python3 - "$OUT/_raw.bin" <<'PY'
import sys, struct
d = open(sys.argv[1], 'rb').read()
w, h, fmt = struct.unpack('<III', d[:12])
off = 12
if len(d) < off + w * h * 4:
    sys.exit(0)
xs, ys = [], []
for y in range(h // 2, h, 3):
    row = off + y * w * 4
    for x in range(0, w, 3):
        r, g, b = d[row + x * 4], d[row + x * 4 + 1], d[row + x * 4 + 2]
        if r > 170 and 70 < g < 175 and b < 70 and r - b > 120:
            xs.append(x); ys.append(y)
if len(xs) > 20:
    print((min(xs) + max(xs)) // 2, (min(ys) + max(ys)) // 2)
PY
}

# Centre of the wide orange primary button of a coaching card (rows where an orange run is at least 600px
# wide; the dino picture is narrower), read from a raw screenshot. Prints "x y" or nothing.
btn_xy() {
  adb exec-out screencap > "$OUT/_raw.bin"
  python3 - "$OUT/_raw.bin" <<'PY'
import sys, struct
d = open(sys.argv[1], 'rb').read()
w, h, fmt = struct.unpack('<III', d[:12])
off = 12
if len(d) < off + w * h * 4:
    sys.exit(0)
rows = []
for y in range(300, h - 300, 2):
    run = best = 0
    base = off + y * w * 4
    for x in range(0, w, 2):
        r, g, b = d[base + x * 4], d[base + x * 4 + 1], d[base + x * 4 + 2]
        if r > 225 and 90 < g < 140 and b < 60:
            run += 2
            best = max(best, run)
        else:
            run = 0
    if best >= 600:
        rows.append(y)
if rows:
    print(w // 2, (rows[0] + rows[-1]) // 2)
PY
}
tap_btn() {  # taps the coaching card's primary button
  clear_overlays
  local xy; xy=$(btn_xy)
  if [ -z "$xy" ]; then xy="540 1700"; fi
  echo "tap button at $xy"
  adb shell input tap $xy
}

# --- Launch: the tutorial starts by itself on a fresh install -----------------------------------
adb shell am start -n $ACT
step t01_launch 14
# The ads consent form (UMP) may sit over the first screen on this emulator; accept it if present.
tap_text "Accept" 540 1678 >/dev/null || true
step t02_intro 4

# --- Intro card -> first drawing ------------------------------------------------------------------
tap_btn
step t03_draw1 4
for i in 1 2 3; do
  adb shell input swipe 250 900 830 1100 250
  adb shell input swipe 830 1300 250 1600 250
done
step t04_draw1_drawn 1

# --- Word 1 is untimed: "Sonraki Kelime" moves on; then the timed words end by themselves --------------
clear_overlays
adb shell input tap 852 2240
step t05_after_word1 3
tap_btn
step t06_draw2 3
for i in 1 2; do adb shell input swipe 250 900 830 1200 250; done
sleep 14
step t07_after_word2 2
tap_btn
step t08_draw3 3
adb shell input swipe 250 900 830 1200 250
sleep 12
step t09_before_guess 2
tap_btn

# --- Guess 1: First Letter lesson ------------------------------------------------------------------
step t10_lesson1 3
echo "ring: $(ring_xy)"
# Everything but the glowing button must be dead: tapping the field / the canvas changes nothing.
adb shell input tap 540 1300
step t11_lesson1_blocked 1
XY=$(ring_xy)
if [ -n "$XY" ]; then adb shell input tap $XY; else adb shell input tap 206 2150; fi
step t12_lesson1_done 2
sleep 4
step t13_lesson1_running 1

# --- Guess 1 answer, then guess 2: Letter Count lesson --------------------------------------------------
adb shell input text "book"
sleep 3
step t14_guess1_answered 1
sleep 3
step t15_lesson2 2
echo "ring: $(ring_xy)"
XY=$(ring_xy)
if [ -n "$XY" ]; then adb shell input tap $XY; else adb shell input tap 540 2150; fi
step t16_lesson2_done 2
adb shell input text "dog"
sleep 4

# --- Guess 3: free choice card, then the third word ----------------------------------------------------
step t17_free_card 2
tap_btn
step t18_guess3 3
adb shell input text "apple"
sleep 4
step t19_finale 3
tap_btn
step t20_home 5

# --- Verdict --------------------------------------------------------------------------------------------
adb logcat -d > "$OUT/logcat.txt"
adb logcat -d -b crash > "$OUT/crash.txt"
FAIL=0
if grep -A 3 "FATAL EXCEPTION" "$OUT/logcat.txt" | grep -q "Process: $PKG"; then
  echo "::error::The app crashed (FATAL EXCEPTION in $PKG)"
  grep -A 14 "FATAL EXCEPTION" "$OUT/logcat.txt" | head -60
  FAIL=1
fi
grep -E "$PKG|AndroidRuntime|FATAL|ANR" "$OUT/logcat.txt" | tail -300 > "$OUT/logcat_app.txt"
rm -f "$OUT/logcat.txt" "$OUT/_tmp.xml" "$OUT/_raw.bin"
exit $FAIL
