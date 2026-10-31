#!/usr/bin/env bash
# Opens the debug-only RunePromoPreviewActivity on each piece that brings the runes forward and takes a screenshot of
# each; on the guided map it also taps the buy button (found by its text in the UI dump) to show the first-rune window.
# Run through .github/workflows/smoke-test.yml with script=runepreview.sh.
set -u

PKG=com.sualtikasifi.cizimhafiza
ACT="$PKG/com.sualtikasifi.cizimhafiza.debug.RunePromoPreviewActivity"
OUT=smoke-out
mkdir -p "$OUT"
APK=$(ls cizim-hafiza/app/build/outputs/apk/debug/*.apk | head -1)
adb wait-for-device
adb shell input keyevent 82 || true
adb shell settings put global hide_error_dialogs 1 || true
sleep 20
adb install -r -t "$APK"
# the game is played in Turkish here: show its string resources in Turkish (per-app language, Android 13+)
adb shell cmd locale set-app-locales "$PKG" --locales tr-TR || true

shot() {
  adb shell am force-stop "$PKG"
  adb shell am start -W -n "$ACT" --es screen "$1"
  sleep "${2:-7}"
  adb exec-out screencap -p > "$OUT/rune_$1.png"
}

# tap the first on-screen element whose text starts with $1 (bounds read from a UI dump)
tap_text() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml "$OUT/ui_$2.xml" >/dev/null 2>&1
  python3 - "$OUT/ui_$2.xml" "$1" <<'PY' > /tmp/tap.txt
import re, sys
xml = open(sys.argv[1], encoding='utf-8').read()
for m in re.finditer(r'<node [^>]*>', xml):
    node = m.group(0)
    t = re.search(r' text="([^"]*)"', node)
    d = re.search(r' content-desc="([^"]*)"', node)
    if (t and t.group(1).startswith(sys.argv[2])) or (d and d.group(1).startswith(sys.argv[2])):
        b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', node)
        x0, y0, x1, y1 = map(int, b.groups())
        print((x0 + x1) // 2, (y0 + y1) // 2)
        break
PY
  if [ -s /tmp/tap.txt ]; then adb shell input tap $(cat /tmp/tap.txt); echo "tapped '$1' at $(cat /tmp/tap.txt)"; else echo "no '$1' on screen"; fi
}

shot home_ready 10
shot home_saving
shot tour_runes
shot funnel
shot runes_guide 8

# the guided map: buy the rune it points at, then the first-rune window
tap_text "Al · " guide
sleep 3
adb exec-out screencap -p > "$OUT/rune_first_bought.png"

# from the home screen, the offer's "Göster" leads to the guided map
shot home_ready 8
tap_text "Göster" home
sleep 5
adb exec-out screencap -p > "$OUT/rune_prompt_to_map.png"

adb logcat -d -t 600 | grep -E "AndroidRuntime|FATAL" > "$OUT/crash.txt" || true
ls -la "$OUT"
