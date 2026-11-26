#!/usr/bin/env bash
# Opens the debug-only ChestPreviewActivity in its "won" state (the "KASA KAZANDIN!" window) for each chest tier and takes a
# screenshot once the pop-in animation has settled. Run through .github/workflows/smoke-test.yml with script=chestwon.sh.
set -u

PKG=com.sualtikasifi.cizimhafiza
OUT=smoke-out
mkdir -p "$OUT"
APK=$(ls cizim-hafiza/app/build/outputs/apk/debug/*.apk | head -1)
adb wait-for-device
adb shell input keyevent 82 || true
adb install -r -t "$APK"
for tier in silver gold rare; do
  adb shell am force-stop "$PKG"
  adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.ChestPreviewActivity" --es tier "$tier" --es state won
  sleep 9
  adb exec-out screencap -p > "$OUT/chestwon_${tier}.png"
done
ls -la "$OUT"
