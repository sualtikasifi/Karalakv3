#!/usr/bin/env bash
# Portuguese and Spanish walk-through, run inside the Google-APIs emulator (see .github/workflows/smoke-test.yml).
# Switches the app's own language with the system per-app locale (API 33+, the same switch the Settings language menu
# makes), then screenshots the main screens in each language so long translations that spill out of a button or a
# painted sign can be spotted. Everything goes to smoke-out/ (published to the ci-smoke-results branch).
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
handle_consent() {
  for i in $(seq 1 ${1:-6}); do
    if [ "$(consent_visible)" = "1" ]; then echo "consent form seen"; adb shell input tap 540 1678; sleep 3; break; fi
    sleep 3
  done
}
dismiss() { tap_text "Wait" || true; sleep 1; }
# open a home tile (coordinates on the 1080x2400 home screen), screenshot it, come back
visit() { adb shell input tap $2 $3; sleep 5; shot "$1"; adb shell input keyevent KEYCODE_BACK; sleep 3; }

# The whole device is switched (root + a framework restart), which is what reliably reaches the app on this image;
# the per-app locale is set as well and both are printed, so a run that stayed in English says why.
set_locale() {
  adb root >/dev/null 2>&1; sleep 3; adb wait-for-device
  adb shell setprop persist.sys.locale "$1"
  adb shell setprop ctl.restart zygote
  sleep 25; adb wait-for-device
  until adb shell pm list packages $PKG 2>/dev/null | grep -q $PKG; do sleep 3; done
  sleep 20
  adb shell cmd locale set-app-locales $PKG --locales "$1" || true
  echo "device locale: $(adb shell getprop persist.sys.locale | tr -d '\r') · app locales: $(adb shell cmd locale get-app-locales $PKG | tr -d '\r')"
}
adb shell settings put global animator_duration_scale 0; adb shell settings put global transition_animation_scale 0; adb shell settings put global window_animation_scale 0
first=1
for LOC in pt-BR es-419; do
  L=${LOC%%-*}
  set_locale $LOC
  adb shell am force-stop $PKG; sleep 2; adb shell am start -n $ACT >/dev/null; sleep 14; dismiss
  if [ $first = 1 ]; then handle_consent 20; first=0; else handle_consent 2; fi
  sleep 2
  shot ${L}_01_home
  visit ${L}_02_race 200 1148
  visit ${L}_03_offline 880 1148
  visit ${L}_04_levels 200 1399
  visit ${L}_05_friends 540 1399
  visit ${L}_06_achievements 880 1399
  visit ${L}_07_league 200 1650
  visit ${L}_08_store 540 1650
  visit ${L}_09_settings 880 1650
  visit ${L}_10_chest_info 980 1855
  # the daily challenge card: the first word of today's challenge in this language
  adb shell input tap 540 870; sleep 6; shot ${L}_11_daily_draw; adb shell input keyevent KEYCODE_BACK; sleep 2; shot ${L}_12_daily_exit; adb shell input keyevent KEYCODE_BACK; sleep 3
done

adb logcat -d | grep -E "FATAL EXCEPTION|ANR in $PKG" -A 20 | head -80 > "$OUT/crash.txt" || true
if [ -s "$OUT/crash.txt" ]; then echo "::error::the app crashed or hit an ANR"; cat "$OUT/crash.txt"; exit 1; fi
echo "done"
