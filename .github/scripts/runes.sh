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
printf '%s\n' "<?xml version='1.0' encoding='utf-8' standalone='yes' ?><map><int name=\"lifetime_xp\" value=\"31000\" /><boolean name=\"tutorial_completed\" value=\"true\" /><boolean name=\"feature_tour_seen\" value=\"true\" /><string name=\"rune_levels\">{&quot;word_gold&quot;:3,&quot;login_gold&quot;:3,&quot;daily_1&quot;:3,&quot;word_2&quot;:3,&quot;win_gold&quot;:3,&quot;login_2&quot;:3,&quot;daily_2&quot;:3,&quot;word_3&quot;:3,&quot;win_2&quot;:3,&quot;login_3&quot;:3,&quot;daily_3&quot;:3,&quot;win_3&quot;:3,&quot;word_key&quot;:1,&quot;word_box&quot;:3,&quot;chest_gold&quot;:3,&quot;star_1&quot;:3,&quot;chest_joker&quot;:3,&quot;chest_2&quot;:3,&quot;star_2&quot;:3,&quot;cjoker_2&quot;:3,&quot;chest_3&quot;:3,&quot;star_3&quot;:3,&quot;cjoker_3&quot;:3,&quot;chest_4&quot;:3,&quot;star_4&quot;:3,&quot;cjoker_key&quot;:1,&quot;chest_5&quot;:3,&quot;ach_1&quot;:3,&quot;disc_1&quot;:3,&quot;ach_2&quot;:3,&quot;disc_2&quot;:3,&quot;dj_1&quot;:3,&quot;ach_3&quot;:3,&quot;disc_3&quot;:3,&quot;ach_4&quot;:3,&quot;dj_key&quot;:1,&quot;disc_4&quot;:3,&quot;word_4&quot;:3,&quot;login_4&quot;:3,&quot;daily_4&quot;:3,&quot;chest_6&quot;:3,&quot;star_5&quot;:3,&quot;cjoker_4&quot;:3,&quot;ach_5&quot;:3,&quot;disc_5&quot;:3,&quot;time_1&quot;:3,&quot;time_2&quot;:3,&quot;ad_1&quot;:3,&quot;time_3&quot;:3,&quot;ad_2&quot;:3,&quot;fr_1&quot;:3,&quot;fr_2&quot;:3,&quot;fr_3&quot;:3,&quot;fr_4&quot;:3}</string></map>" \
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
# Pan around the map: north (gold), south, west (chests), east (friends/luck), taking a screenshot each time.
adb shell input swipe 540 600 540 1700 500; sleep 2; shot r04_north
adb shell input swipe 540 600 540 1700 500; sleep 2; shot r05_far_north
adb shell input swipe 540 1700 540 400 500; sleep 1; adb shell input swipe 540 1700 540 400 500; sleep 1; adb shell input swipe 540 1700 540 400 500; sleep 1; adb shell input swipe 540 1700 540 400 500; sleep 2; shot r06_south
adb shell input swipe 200 1000 1000 1000 500; sleep 1; adb shell input swipe 200 1000 1000 1000 500; sleep 2; shot r07_west
adb shell input swipe 1000 1000 100 1000 500; sleep 1; adb shell input swipe 1000 1000 100 1000 500; sleep 1; adb shell input swipe 1000 1000 100 1000 500; sleep 1; adb shell input swipe 1000 1000 100 1000 500; sleep 2; shot r08_east
adb logcat -d | grep -E "FATAL|ANR in" | head -5 > "$OUT/crash.txt" || true
