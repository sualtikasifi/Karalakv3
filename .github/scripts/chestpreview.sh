#!/usr/bin/env bash
# Opens the debug-only ChestPreviewActivity (the chest window on its own) in several states and takes a screenshot of each.
# Run through .github/workflows/smoke-test.yml with script=chestpreview.sh.
set -u

PKG=com.sualtikasifi.cizimhafiza
OUT=smoke-out
mkdir -p "$OUT"
APK=$(ls cizim-hafiza/app/build/outputs/apk/debug/*.apk | head -1)
adb wait-for-device
adb shell input keyevent 82 || true
adb install -r -t "$APK"
for combo in silver:start gold:unlocking rare:unlocking rare:ready gold:busy silver:used; do
  tier=${combo%%:*}; state=${combo##*:}
  adb shell am force-stop "$PKG"
  adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.ChestPreviewActivity" --es tier "$tier" --es state "$state"
  sleep 5
  adb exec-out screencap -p > "$OUT/chest_${tier}_${state}.png"
done
ls -la "$OUT"
