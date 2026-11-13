#!/usr/bin/env bash
# Launches the release APK on the Pixel 9 emulator profile and captures the reading screen in portrait and landscape,
# the left navigation, a long press on John 1:5 (its footnote) and the start of John 2 in dark mode, to check that the
# toolbar and chapter picker stay inside the system bars.
set -u

APP_ID="com.github.danzuep.yeshua.android"
APP_NAMESPACE="com.github.danzuep.yeshua.android"

dump_ui() {
  rm -f ui.xml
  adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1
  adb pull /sdcard/ui.xml ui.xml > /dev/null 2>&1
}

# Prints "x y" inside the first node whose text matches $1; $2 picks the point: "start" (left, near the bottom) or "center".
find_text() {
  TEXT="$1" WHERE="$2" python3 - << 'PY'
import os, re
xml = open("ui.xml", encoding="utf-8").read()
node = re.search(r'<node[^>]*text="[^"]*' + os.environ["TEXT"] + r'[^"]*"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
if node:
    x1, y1, x2, y2 = map(int, node.groups())
    print((x1 + x2) // 2 if os.environ["WHERE"] == "center" else x1 + 150, (y1 + y2) // 2 if os.environ["WHERE"] == "center" else y2 - 35)
PY
}

open_john() {
  adb shell am start -W --activity-single-top -n "$APP_ID/$APP_NAMESPACE.LauncherActivity" --ei "$APP_NAMESPACE.KEY_OPEN_BOOK" 42 > /dev/null
  sleep 5
}

adb install app/build/outputs/apk/release/app-release.apk
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
open_john
# The bundled translation is imported on first launch.
sleep 25
open_john

adb exec-out screencap -p > pixel9-1-reading-portrait.png
adb shell settings put system user_rotation 1
sleep 3
adb exec-out screencap -p > pixel9-1-reading-landscape.png
adb shell settings put system user_rotation 0
sleep 3

# The title opens the left navigation.
dump_ui
xy=$(find_text "John 1" center)
if [ -n "$xy" ]; then adb shell input tap $xy; else echo "no title found"; fi
sleep 2
adb exec-out screencap -p > pixel9-2-navigation.png
adb shell input keyevent KEYCODE_BACK
sleep 1

# Long press the last line of the paragraph holding verses 1-5, which is verse 5 and has the footnote.
dump_ui
xy=$(find_text "The light shines in the darkness" start)
if [ -n "$xy" ]; then adb shell input swipe $xy $xy 1200; else echo "no verse 5 found"; fi
sleep 2
adb exec-out screencap -p > pixel9-3-footnote.png
dump_ui
cp ui.xml pixel9-3-footnote.xml
adb shell input keyevent KEYCODE_BACK
sleep 1

# Dark mode, scrolled to the start of John 2.
adb shell cmd uimode night yes
sleep 3
open_john
read -r width height < <(adb shell wm size | grep -oE '[0-9]+x[0-9]+' | tail -n 1 | tr 'x' ' ')
for _ in 1 2 3 4 5 6 7 8 9 10 11 12; do
  adb shell input swipe $((width / 2)) $((height * 3 / 4)) $((width / 2)) $((height / 4)) 300
  sleep 1
  dump_ui
  if grep -q "¶2" ui.xml 2> /dev/null; then break; fi
done
adb exec-out screencap -p > pixel9-4-chapter-dark.png
adb logcat -d > logcat.txt
