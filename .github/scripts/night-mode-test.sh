#!/usr/bin/env bash
# Installs the debug APK, simulates a camera cutout, then screenshots the reading screen in night mode and in light mode.
set -u

APP_NAMESPACE="com.github.danzuep.yeshua.android"
APP_ID="$APP_NAMESPACE.debug"

adb install app/build/outputs/apk/debug/app-debug.apk
adb shell cmd overlay enable com.android.internal.display.cutout.emulation.tall

open_reading() {
  adb shell am start -W --activity-single-top -n "$APP_ID/$APP_NAMESPACE.LauncherActivity" --ei "$APP_NAMESPACE.KEY_OPEN_BOOK" "$1"
  sleep "$2"
}

# The first launch imports the bundled translation, so give it time.
open_reading 0 20

adb shell cmd uimode night yes
sleep 3
open_reading 42 8
adb exec-out screencap -p > night.png

adb shell cmd uimode night no
sleep 3
open_reading 42 8
adb exec-out screencap -p > night-light.png
