#!/usr/bin/env bash
# Runs the per-screen scripts one after another in the same emulator, keeping each one's screenshots in its own
# folder, so one run gives a picture of (almost) every screen for a visual review. A script that fails does not
# stop the others; its folder still holds whatever it shot before failing.
set -u
ALL=smoke-out-all
rm -rf "$ALL"; mkdir -p "$ALL"
for s in splash home titles pages game draw store runes chest worlds league achievements friends duel tutorial; do
  echo "=== $s ==="
  rm -rf smoke-out
  timeout 600 bash ".github/scripts/$s.sh" > "$ALL/$s.log" 2>&1; echo "exit=$?" >> "$ALL/$s.log"
  mkdir -p "$ALL/$s"
  [ -d smoke-out ] && cp -r smoke-out/. "$ALL/$s/"
  tail -3 "$ALL/$s.log"
done
rm -rf smoke-out; mv "$ALL" smoke-out
exit 0
