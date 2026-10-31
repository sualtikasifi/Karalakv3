#!/usr/bin/env bash
# Opens the debug-only DevPanelPreviewActivity on the passcode screen and on each listed tab of the real developer panel,
# and takes a screenshot of each. Run through .github/workflows/smoke-test.yml with script=devpanelpreview.sh.
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
# the panel is used in Turkish: show its string resources in Turkish too (per-app language, Android 13+)
adb shell cmd locale set-app-locales "$PKG" --locales tr-TR || true

shot() {
  # a fresh start each time: an open activity would only be handed the new intent
  adb shell am force-stop "$PKG"
  adb shell am start -W -n "$ACT" --es screen "$1"
  sleep "${2:-7}"
  adb exec-out screencap -p > "$OUT/devpanel_$1.png"
}

# warm-up: the first start pays for dex loading and Firebase, so its frame is not one to judge
shot Dashboard 10
for s in gate Dashboard Tools Queue Stats Assistant Studio Coupons XpEvent League Players Economy Announce Push Feedback Locks Update Jobs; do
  shot "$s"
done

# the passcode screen with a wrong code typed in (the shake and the error line)
adb shell am force-stop "$PKG"
adb shell am start -W -n "$ACT" --es screen gate
sleep 6
adb exec-out screencap -p > "$OUT/devpanel_gate_open.png"

# a tool, then back: the tools grid reached from the bottom bar, scrolled
adb shell am force-stop "$PKG"
adb shell am start -W -n "$ACT" --es screen Tools
sleep 6
adb shell input swipe 540 1700 540 700 400
sleep 2
adb exec-out screencap -p > "$OUT/devpanel_Tools_scrolled.png"

adb logcat -d -t 400 | grep -E "AndroidRuntime|FATAL" > "$OUT/crash.txt" || true
ls -la "$OUT"
