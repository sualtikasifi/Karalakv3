#!/usr/bin/env bash
# Opens the debug-only LobbyPreviewActivity (the lobby's 8 seats in the space the real lobby gives them) and shoots it
# with 2 and with 8 players. Run through .github/workflows/smoke-test.yml with script=lobbypreview.sh.
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
for n in 2 8; do
  adb shell am force-stop "$PKG"
  adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.LobbyPreviewActivity" --ei players "$n"
  sleep 6
  adb exec-out screencap -p > "$OUT/lobby_$n.png"
done
# The same 8 seats squeezed the way a lobby with the ready button squeezes them.
adb shell am force-stop "$PKG"
adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.LobbyPreviewActivity" --ei players 8 --ei grid 188
sleep 6
adb exec-out screencap -p > "$OUT/lobby_8_short.png"
# The ready button once everyone is ready and the host has not started: two lines inside the green pill.
adb shell am force-stop "$PKG"
adb shell am start -W -n "$PKG/com.sualtikasifi.cizimhafiza.debug.LobbyPreviewActivity" --ez button true
sleep 6
adb exec-out screencap -p > "$OUT/lobby_button.png"
ls -la "$OUT"
