#!/usr/bin/env bash
# Launches the release APK on the Pixel 10 emulator profile and captures the reading screen in portrait and landscape,
# to check that the toolbar and chapter picker stay inside the system bars.
set -u

APP_ID="com.github.danzuep.yeshua.android"
APP_NAMESPACE="com.github.danzuep.yeshua.android"

adb install app/build/outputs/apk/release/app-release.apk
adb shell settings put system accelerometer_rotation 0
adb shell am start -W --activity-single-top -n "$APP_ID/$APP_NAMESPACE.LauncherActivity" --ei "$APP_NAMESPACE.KEY_OPEN_BOOK" 42
# The bundled translation is imported on first launch.
sleep 30

capture() {
  adb exec-out screencap -p > "pixel10-$1.png"
  adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1
  adb pull /sdcard/ui.xml "pixel10-$1.xml" > /dev/null 2>&1
}

adb shell settings put system user_rotation 0
sleep 3
capture portrait
adb shell settings put system user_rotation 1
sleep 3
capture landscape
adb logcat -d > logcat.txt
