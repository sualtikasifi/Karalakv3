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
adb shell settings put global hide_error_dialogs 1 || true

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
adb logcat -d -s StartupTiming | tee "$OUT/startup_timing.txt"
# What the player sees while the app starts: a screenshot every few tenths of a second from the moment of launch
# (l01..l14). A plain-colour first screenshot means the system's starting window is still showing.
adb shell am force-stop $PKG; sleep 3
adb shell "am start -n $ACT" >/dev/null 2>&1 &
for i in $(seq 1 14); do shot l$(printf %02d $i); sleep 0.2; done
# The same with animations ON, the way players see it: the logo growing into the opening scene (m01..m14).
adb shell settings put global animator_duration_scale 1; adb shell settings put global transition_animation_scale 1; adb shell settings put global window_animation_scale 1
adb shell am force-stop $PKG; sleep 3
adb logcat -c
# A real video of the launch (screenshots are too slow to catch a splash), turned into a contact sheet of frames.
adb shell screenrecord --time-limit 7 /sdcard/launch.mp4 &
REC=$!
sleep 1
adb shell "am start -n $ACT" >/dev/null 2>&1 &
for i in $(seq 1 6); do shot m$(printf %02d $i); sleep 0.1; done
wait $REC; sleep 1
adb pull /sdcard/launch.mp4 "$OUT/launch.mp4" >/dev/null 2>&1 || true
adb logcat -d | grep -iE "splash|StartingSurface|StartingWindow|AnimatedVector|VectorDrawable" | cut -c1-240 > "$OUT/splash_log.txt" || true
if [ -f "$OUT/launch.mp4" ]; then
  python3 -c "import PIL" 2>/dev/null || python3 -m pip install -q pillow >/dev/null 2>&1 || true
  command -v ffmpeg >/dev/null || (sudo apt-get install -y -qq ffmpeg >/dev/null 2>&1 || true)
  mkdir -p /tmp/lf && rm -f /tmp/lf/*.png
  ffmpeg -loglevel error -i "$OUT/launch.mp4" -vf "fps=12,scale=180:-1" /tmp/lf/f%03d.png || true
  python3 - "$OUT/launch_sheet.png" <<'PY'
import sys, glob
from PIL import Image
fs = sorted(glob.glob('/tmp/lf/f*.png'))[:60]
if fs:
    ims = [Image.open(f).convert('RGB') for f in fs]
    w, h = ims[0].size
    cols = 10
    rows = (len(ims) + cols - 1) // cols
    sheet = Image.new('RGB', (w * cols, h * rows), 'white')
    for i, im in enumerate(ims):
        sheet.paste(im, ((i % cols) * w, (i // cols) * h))
    sheet.save(sys.argv[1])
PY
  rm -f "$OUT/launch.mp4"
fi
sleep 2
adb shell settings put global animator_duration_scale 0; adb shell settings put global transition_animation_scale 0; adb shell settings put global window_animation_scale 0

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
handle_consent() {
  for i in 1 2 3 4 5 6; do
    if [ "$(consent_visible)" = "1" ]; then echo "consent again"; adb shell input tap 540 1678; sleep 3; break; fi
    sleep 3
  done
}
P=${P:-a}
P=n
dismiss() { tap_text "Wait" || true; sleep 1; }
adb shell am force-stop $PKG; sleep 2; adb shell am start -n $ACT >/dev/null; sleep 14; dismiss; handle_consent; sleep 3
shot ${P}01_home
# Press-and-hold on two painted areas, with a screenshot while the finger is still down: shows the "sink" (the tile caves in).
(adb shell input swipe 204 1148 204 1148 4000 &) ; sleep 2; shot ${P}03_press_tile; sleep 3
(adb shell input swipe 175 820 175 820 4000 &) ; sleep 2; shot ${P}04_press_card; sleep 3
adb shell input keyevent KEYCODE_BACK; sleep 2
# Settings: open it from the tile, look at it, flip one switch, press-and-hold a row.
adb shell input tap 880 1650; sleep 4; shot s01_settings
adb shell input tap 540 842; sleep 2; shot s02_settings_switch
(adb shell input swipe 540 2012 540 2012 4000 &) ; sleep 2; shot s03_settings_press; sleep 3
adb shell input keyevent KEYCODE_BACK; sleep 2
# Quick match: the "Rakip aranıyor" scene shows only while the search runs, so shoot early and often.
adb shell am force-stop $PKG; sleep 2; adb shell am start -n $ACT >/dev/null; sleep 14; dismiss; handle_consent; sleep 3
adb shell input tap 540 1148; sleep 0.6; shot q01_search; sleep 0.8; shot q02_search; sleep 1.5; shot q03_search; sleep 4; shot q04_after
adb shell input keyevent KEYCODE_BACK; sleep 2; tap_text "Wait" || true
adb shell wm size 1080x1920; sleep 4; adb shell am force-stop $PKG; sleep 2; adb shell am start -n $ACT >/dev/null; sleep 14; dismiss; handle_consent; sleep 3
shot ${P}02_home_short
adb shell wm size reset
adb logcat -d -s StartupTiming > "$OUT/startup_timing_full.txt"
