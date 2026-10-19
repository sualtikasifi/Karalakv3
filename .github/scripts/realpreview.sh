#!/usr/bin/env bash
# Draws frames of the studio's "Gerçek Olsaydı" video in the emulator (the debug-only RealPreviewActivity, from made-up
# drawings and a made-up clip) and pulls the PNGs out, so the video's design can be looked at without the developer
# panel. The results go to smoke-out/ like the other scripts'.
set -u
PKG=com.sualtikasifi.cizimhafiza
OUT=smoke-out
mkdir -p "$OUT"
APK=$(ls cizim-hafiza/app/build/outputs/apk/debug/*.apk | head -1)
adb wait-for-device
adb shell input keyevent 82 || true
adb install -r -t "$APK"
adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.RealPreviewActivity"
for i in $(seq 1 120); do
  if adb shell "run-as $PKG ls files/preview/done.txt" 2>/dev/null | grep -q done.txt; then echo "done after $((i * 2))s"; break; fi
  sleep 2
done
for f in $(adb shell "run-as $PKG ls files/preview" | tr -d '\r'); do
  # through base64: a raw binary pipe of the last file was now and then cut short
  adb shell "run-as $PKG base64 files/preview/$f" | base64 -d > "$OUT/$f"
done
ls -la "$OUT"
