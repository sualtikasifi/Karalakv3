#!/usr/bin/env bash
# A brand-new install, as a player meets it: launches the app with no data and takes screenshots over time and after
# skipping the tutorial, to look at the opening steps. Run through .github/workflows/smoke-test.yml with script=firstrun.sh.
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
shot() { adb exec-out screencap -p > "$OUT/fr_$1.png"; }
dump() { adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml "$OUT/fr_$1.xml" >/dev/null 2>&1; }
start=$(date +%s)
adb shell am start -n $ACT >/dev/null
for t in 2 5 9 14; do
  now=$(( $(date +%s) - start )); [ $now -lt $t ] && sleep $(( t - now ))
  shot "t${t}"
done
dump tut1
sleep 3
shot tut2
# the tutorial's skip button, wherever its text is exposed
for label in "Skip" "Atla"; do
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml "$OUT/_u.xml" >/dev/null 2>&1
  xy=$(python3 - "$OUT/_u.xml" "$label" <<'PY'
import re, sys
x = open(sys.argv[1], encoding='utf-8').read()
for m in re.finditer(r'(?:text|content-desc)="([^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', x):
    if m.group(1).strip().lower().startswith(sys.argv[2].lower()):
        a = list(map(int, m.groups()[1:])); print((a[0]+a[2])//2, (a[1]+a[3])//2); break
PY
)
  if [ -n "$xy" ]; then echo "tap $label at $xy"; adb shell input tap $xy; break; fi
done
sleep 4; shot after_skip1
sleep 4; shot after_skip2
sleep 6; shot after_skip3
dump after_skip
adb logcat -d | grep -E "FATAL|AndroidRuntime" | head -20 > "$OUT/crash.txt" || true
ls -la "$OUT"
