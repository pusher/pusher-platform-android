#!/usr/bin/env bash

android list targets
echo no | android create avd --force -n test -k "system-images;android-27;google_apis;x86"
emulator -avd test -no-skin -no-audio -no-window &
android-wait-for-emulator
adb shell input keyevent 82 &
