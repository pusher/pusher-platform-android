#!/usr/bin/env bash

echo "Running ${BASH_SOURCE[0]}"

./gradlew assembleDebug assembleDebugAndroidTest

source $HOME/google-cloud-sdk/path.bash.inc
gcloud version
gcloud componets list

gcloud firebase test android run \
  --type instrumentation \
  --app ./gradle/firebase/dummy.apk \
  --test ./library/build/outputs/apk/androidTest/debug/library-debug-androidTest.apk \
  --device-ids Nexus5 \
  --os-version-ids 22 \
  --locales en_GB \
  --orientations portrait \
  --environment-variables coverage=true,coverageFile="/sdcard/coverage.ec" \
  --directories-to-pull=/sdcard \
  --project pusher-platform-android

