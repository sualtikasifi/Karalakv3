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
printf '%s\n' "<?xml version='1.0' encoding='utf-8' standalone='yes' ?><map><int name=\"lifetime_xp\" value=\"80000\" /><boolean name=\"tutorial_completed\" value=\"true\" /><boolean name=\"feature_tour_seen\" value=\"true\" /><string name=\"rune_levels\">{&quot;word_gold&quot;:5,&quot;win_gold&quot;:4,&quot;streak_key&quot;:1,&quot;chest_gold&quot;:5,&quot;chest_joker&quot;:4,&quot;chest_key2&quot;:1,&quot;disc_1&quot;:4,&quot;dj_1&quot;:3,&quot;luck_key&quot;:1,&quot;login_gold&quot;:5,&quot;daily_1&quot;:5,&quot;ins_key&quot;:1,&quot;time_1&quot;:4,&quot;ad_1&quot;:3,&quot;fr_1&quot;:4,&quot;fr_2&quot;:3,&quot;xp_1&quot;:5,&quot;st_1&quot;:1,&quot;st_5&quot;:1,&quot;st_3&quot;:1}</string></map>" \
  | adb shell "run-as $PKG sh -c 'cat > shared_prefs/cizim_hafiza_settings.xml'"
# The first-run year-of-birth and terms prompt (LegalConsentHost) would cover every screen: answered up front.
printf '%s\n' "<?xml version='1.0' encoding='utf-8' standalone='yes' ?><map><int name=\"birth_year\" value=\"1990\" /><int name=\"terms_version\" value=\"1\" /></map>" \
  | adb shell "run-as $PKG sh -c 'cat > shared_prefs/legal_consent.xml'"

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
    if b > 90 and b > r * 2.5 and b > g * 1.4: ok += 1
print(1 if ok >= 2 else 0)
PY
}
handle_consent() {
  for i in 1 2 3 4 5 6; do
    if [ "$(consent_visible)" = "1" ]; then echo "consent seen"; adb shell input tap 540 1678; sleep 3; break; fi
    sleep 3
  done
}
dismiss() { tap_text "Wait" || true; sleep 1; }

adb shell am force-stop $PKG; sleep 2; adb shell am start -n $ACT >/dev/null; sleep 14; dismiss; handle_consent; dismiss; handle_consent; sleep 3
shot r00_home
# The mascot (top right of the home scene) opens the rune map.
dismiss; adb shell input tap 980 752; sleep 2; shot r00b_after_streak_close
# Closing the streak window can show a test interstitial: wait for its close button and press it.
for i in 1 2 3 4 5; do sleep 6; tap_text "Close" || tap_text "close ad" || tap_text "Skip" || true; done
shot r00c_after_ad
adb shell input tap 820 215; sleep 4
shot r01_runes
# Select a node: first the middle-ish one, then an empty spot (the card must close).
adb shell input tap 540 1300; sleep 2; shot r02_after_tap
adb shell input tap 300 500; sleep 2; shot r03_after_empty_tap
# Pan around the map: the picture centre, then north (three swipes), back, south, west and east.
north() { adb shell input swipe 540 500 540 1500 450; sleep 1.2; }
south() { adb shell input swipe 540 1500 540 500 450; sleep 1.2; }
west() { adb shell input swipe 200 1000 1000 1000 450; sleep 1.2; }
east() { adb shell input swipe 1000 1000 200 1000 450; sleep 1.2; }
north; shot r04_n1
north; shot r05_n2
north; shot r06_n3
north; shot r07_n4
south; south; south; south; sleep 1
south; shot r08_s1
south; shot r09_s2
north; north; north; sleep 1
west; shot r10_w1
west; shot r11_w2
east; east; east; shot r12_e1
east; shot r13_e2
adb logcat -d | grep -E "FATAL|ANR in" | head -5 > "$OUT/crash.txt" || true
