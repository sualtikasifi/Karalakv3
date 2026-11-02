#!/usr/bin/env bash
# A new install with the app's language set to Turkish (per-app locale, the phone itself stays English): gets through the
# tutorial skip and the date-of-birth window and screenshots the ad consent form, to see which language it speaks.
# Run through .github/workflows/smoke-test.yml with script=consentlang.sh.
set -u

PKG=com.sualtikasifi.cizimhafiza
ACT=$PKG/.presentation.MainActivity
OUT=smoke-out
mkdir -p "$OUT"
APK=$(ls cizim-hafiza/app/build/outputs/apk/debug/*.apk | head -1)
adb wait-for-device
adb shell input keyevent 82 || true
adb shell settings put global hide_error_dialogs 1 || true
adb shell settings put global animator_duration_scale 1 || true
adb shell settings put global transition_animation_scale 1 || true
adb shell settings put global window_animation_scale 1 || true
sleep 20
adb install -r -t "$APK"
adb shell pm clear $PKG >/dev/null 2>&1 || true
adb shell cmd locale set-app-locales "$PKG" --locales tr-TR || true
shot() { adb exec-out screencap -p > "$OUT/cl_$1.png"; }

# taps the first node whose text/content-desc contains $1 (case-insensitive); prints nothing and returns 1 when none
tap_text() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml "$OUT/_u.xml" >/dev/null 2>&1
  local xy
  xy=$(python3 - "$OUT/_u.xml" "$1" <<'PY'
import re, sys
x = open(sys.argv[1], encoding='utf-8').read()
for m in re.finditer(r'<node [^>]*>', x):
    n = m.group(0)
    t = (re.search(r' text="([^"]*)"', n) or [None, ''])[1] + ' ' + (re.search(r' content-desc="([^"]*)"', n) or [None, ''])[1]
    if sys.argv[2].lower() in t.lower():
        b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n)
        a = list(map(int, b.groups())); print((a[0]+a[2])//2, (a[1]+a[3])//2); break
PY
)
  if [ -n "$xy" ]; then echo "tap '$1' at $xy"; adb shell input tap $xy; return 0; fi
  echo "NOT FOUND '$1'"; return 1
}

adb shell am start -n $ACT >/dev/null
sleep 18
shot welcome
tap_text "atla" || tap_text "skip"
sleep 6
shot dob
adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml "$OUT/cl_dob.xml" >/dev/null 2>&1
# the first of the three number fields, then the digits: each field hands over to the next once it is full
xy=$(python3 - "$OUT/cl_dob.xml" <<'PY'
import re, sys
x = open(sys.argv[1], encoding='utf-8').read()
for m in re.finditer(r'<node [^>]*class="android.widget.EditText"[^>]*>', x):
    b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', m.group(0))
    a = list(map(int, b.groups())); print((a[0]+a[2])//2, (a[1]+a[3])//2); break
PY
)
echo "first field: $xy"
[ -n "$xy" ] && adb shell input tap $xy
sleep 1
for part in 15 06 2000; do adb shell input text $part; sleep 1; done
shot dob_filled
adb shell input keyevent 4   # closes the keyboard, if open
sleep 1
tap_text "devam" || tap_text "continue"
sleep 20
shot after_dob_1
sleep 30
shot after_dob_2
sleep 40
shot after_dob_3
adb logcat -d | grep -iE 'consent|UMP|ump' | head -40 > "$OUT/consent_log.txt" || true
adb logcat -d | grep -E "FATAL" | head -5 > "$OUT/crash.txt" || true
ls -la "$OUT"
