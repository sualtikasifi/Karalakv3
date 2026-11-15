#!/usr/bin/env bash
# Opens the debug-only DuelResultPreviewActivity (an answered duel's result screen), then switches the gallery to the challenger's drawings.
# Run through .github/workflows/smoke-test.yml with script=duelresultpreview.sh.
set -u

PKG=com.sualtikasifi.cizimhafiza
OUT=smoke-out
mkdir -p "$OUT"
APK=$(ls cizim-hafiza/app/build/outputs/apk/debug/*.apk | head -1)
adb wait-for-device
adb shell input keyevent 82 || true
adb shell settings put global hide_error_dialogs 1 || true
sleep 20
adb install -r -t "$APK"
adb shell am force-stop "$PKG"
adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.DuelResultPreviewActivity"
sleep 5
adb exec-out screencap -p > "$OUT/duel_result_1.png"
# The challenger's chip: found by its text in the UI tree, tapped at its centre.
adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1 || true
adb pull /sdcard/ui.xml "$OUT/ui.xml" >/dev/null 2>&1 || true
XY=$(python3 - "$OUT/ui.xml" <<'PY'
import re, sys
try:
    s = open(sys.argv[1], encoding="utf-8").read()
    for m in re.finditer(r'text="liliput"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', s):
        x0, y0, x1, y1 = map(int, m.groups())
        print((x0 + x1) // 2, (y0 + y1) // 2)
except Exception:
    pass
PY
)
echo "chip at: $XY"
[ -n "$XY" ] && adb shell input tap $XY
sleep 2
adb exec-out screencap -p > "$OUT/duel_result_2.png"
adb logcat -d | grep -E "FATAL|AndroidRuntime" | head -40 > "$OUT/crash.txt" || true
ls -la "$OUT"
