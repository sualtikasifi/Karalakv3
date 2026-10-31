#!/usr/bin/env bash
# Opens the debug-only DevPanelPreviewActivity (the developer panel's lobby background: the passcode screen and a stand-in
# working screen) and takes a screenshot of each. Run through .github/workflows/smoke-test.yml with script=devpanelpreview.sh.
set -u

PKG=com.sualtikasifi.cizimhafiza
ACT="$PKG/com.sualtikasifi.cizimhafiza.debug.DevPanelPreviewActivity"
OUT=smoke-out
mkdir -p "$OUT"
APK=$(ls cizim-hafiza/app/build/outputs/apk/debug/*.apk | head -1)
adb wait-for-device
adb shell input keyevent 82 || true
adb shell settings put global hide_error_dialogs 1 || true
sleep 20
adb install -r -t "$APK"
adb shell am start -W -n "$ACT" --es screen gate
sleep 6
adb exec-out screencap -p > "$OUT/devpanel_gate.png"
# a fresh start: the activity is already open on the passcode screen and would only be handed the new intent
adb shell am force-stop "$PKG"
adb shell am start -W -n "$ACT" --es screen panel
sleep 6
adb exec-out screencap -p > "$OUT/devpanel_panel.png"
ls -la "$OUT"
