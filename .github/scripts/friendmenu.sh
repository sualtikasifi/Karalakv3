#!/usr/bin/env bash
# Opens the debug-only FriendsPreviewActivity (the friends page with seven friends), then taps the first row's three dots and takes a screenshot of the menu.
# Run through .github/workflows/smoke-test.yml with script=friendmenu.sh.
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
adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.FriendsPreviewActivity"
sleep 8
adb exec-out screencap -p > "$OUT/friends_page.png"
find_xy() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml "$OUT/_tmp.xml" >/dev/null 2>&1
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
xy=$(find_xy "more actions")
echo "menu button at: $xy"
[ -n "$xy" ] && { adb shell input tap $xy; sleep 3; }
adb exec-out screencap -p > "$OUT/friend_menu.png"
ls -la "$OUT"
