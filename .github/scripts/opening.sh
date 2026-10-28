#!/usr/bin/env bash
# The opening of the app, in an emulator: a player who already finished the tutorial updates to a version with the
# date-of-birth prompt and has not seen "Yenilikler" yet. The opening prompts must come one at a time (StartupPrompts):
#  1) only the date-of-birth prompt (nothing behind it, no notification permission);
#  2) after it, the ad consent form (debug builds act as if in the EEA);
#  3) after that, "Yenilikler" alone;
#  4) no notification permission in that launch; on the next, quiet launch it is asked once.
# Every step is screenshotted; whether a system permission dialog is up is written to smoke-out/opening_log.txt.
# Run through .github/workflows/smoke-test.yml with script=opening.sh.
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
# Prints "x y" of the first node whose text or content-desc contains $1 (case-insensitive); with a second
# argument "exact", only a node whose text or content-desc is exactly $1.
find_xy() {
  dump_ui _tmp
  python3 - "$1" "$OUT/_tmp.xml" "${2:-}" <<'PY'
import sys, re, xml.etree.ElementTree as ET
label, path, exact = sys.argv[1].lower(), sys.argv[2], sys.argv[3] == "exact"
try:
    tree = ET.parse(path)
except Exception:
    sys.exit(0)
for n in tree.iter('node'):
    texts = [(n.get(k) or '').strip().lower() for k in ('text', 'content-desc', 'hint')]
    if (label in texts) if exact else (label in ' '.join(texts)):
        m = re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', n.get('bounds') or '')
        if m:
            x1, y1, x2, y2 = map(int, m.groups())
            print((x1 + x2) // 2, (y1 + y2) // 2)
            break
PY
}
tap_label() { local xy; xy=$(find_xy "$1"); [ -n "$xy" ] && { echo "tap '$1' at $xy"; adb shell input tap $xy; return 0; }; echo "NOT FOUND: '$1'"; return 1; }
tap_exact() { local xy; xy=$(find_xy "$1" exact); [ -n "$xy" ] && { echo "tap '$1' at $xy"; adb shell input tap $xy; return 0; }; echo "NOT FOUND: '$1'"; return 1; }
# The slow emulator sometimes puts a system "Pixel Launcher isn't responding" dialog over the app, which swallows
# every tap; its "Wait" button dismisses it.
clear_anr() {
  local xy i
  for i in 1 2 3; do
    xy=$(find_xy "Wait" exact)
    [ -z "$xy" ] && return 0
    echo "system dialog: tapping 'Wait' at $xy"; adb shell input tap $xy; sleep 2
  done
}

# Whether the system's notification permission dialog is on screen right now.
permission_up() { dump_ui _perm; grep -q "permissioncontroller" "$OUT/_perm.xml" && echo yes || echo no; }
log() { echo "$*" | tee -a "$OUT/opening_log.txt"; }

adb uninstall $PKG >/dev/null 2>&1 || true
adb install -r -t "$APK"
adb shell settings put global hide_error_dialogs 1 || true
# An existing player: the tutorial is done, "Yenilikler" not seen, no date of birth yet. The notification permission
# is left ungranted on purpose: the point is when (and whether) the app asks for it.
adb shell "run-as $PKG mkdir -p shared_prefs"
printf '%s\n' "<?xml version='1.0' encoding='utf-8' standalone='yes' ?><map><boolean name=\"tutorial_completed\" value=\"true\" /></map>" \
  | adb shell "run-as $PKG sh -c 'cat > shared_prefs/cizim_hafiza_settings.xml'"
adb logcat -c
adb shell am start -n $ACT
sleep 16
clear_anr
shot "opening_01_first"
log "1) date-of-birth prompt up: $( [ -n "$(find_xy "YYYY" exact)" ] && echo yes || echo no ); permission dialog: $(permission_up)"

tap_exact "DD" || tap_exact "GG"
sleep 1; adb shell input text "15"; sleep 1
tap_exact "MM" || tap_exact "AA"
sleep 1; adb shell input text "06"; sleep 1
tap_exact "YYYY" || tap_exact "AAAA"
sleep 1; adb shell input text "1990"; sleep 1
adb shell input keyevent KEYCODE_BACK
sleep 1
tap_label "Continue" || tap_label "Devam"
sleep 6
clear_anr
shot "opening_02_after_birth_date"
log "2) after the date: permission dialog: $(permission_up)"
tap_label "Consent" || true
sleep 6
clear_anr
shot "opening_03_after_consent"
log "3) after consent: permission dialog: $(permission_up)"
sleep 6
shot "opening_04_settled"
log "4) a while later: permission dialog: $(permission_up)"
# Through "Yenilikler" (its close button, or its last page's button).
for i in 1 2 3 4 5 6 7 8; do tap_exact "Next" || tap_label "Let's go" || break; sleep 2; done
sleep 4
shot "opening_05_after_tour"
log "5) after the tour: permission dialog: $(permission_up)"

# The next launch is a quiet one: the permission is asked once, a few seconds in.
adb shell am force-stop $PKG
sleep 2
adb shell am start -n $ACT
sleep 20
clear_anr
shot "opening_06_next_launch"
log "6) next launch: permission dialog: $(permission_up)"

adb logcat -d > "$OUT/opening_logcat.txt"
FAIL=0
if grep -A 3 "FATAL EXCEPTION" "$OUT/opening_logcat.txt" | grep -q "Process: $PKG"; then
  echo "::error::The app crashed"; grep -A 14 "FATAL EXCEPTION" "$OUT/opening_logcat.txt" | head -40; FAIL=1
fi
grep -E "$PKG|AndroidRuntime|FATAL" "$OUT/opening_logcat.txt" | tail -200 > "$OUT/opening_logcat_app.txt"; rm -f "$OUT/opening_logcat.txt" "$OUT"/_*.xml
exit $FAIL
