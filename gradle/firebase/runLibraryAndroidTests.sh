#!/usr/bin/env bash

./gradlew assembleDebugTest

gcloud firebase test android run \
  --type instrumentation \
  --app ./firebase/dummy.apk \
  --test ./library/build/outputs/apk/androidTest/debug/library-debug-androidTest.apk \
  --device-ids Nexus5 \
  --os-version-ids 22 \
  --locales en_GB \
  --orientations portrait \
  --environment-variables coverage=true,coverageFile="/sdcard/coverage.ec" \
  --directories-to-pull=/sdcard \
  --project pusher-platform-android

