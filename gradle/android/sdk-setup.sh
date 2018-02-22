#!/usr/bin/env bash

echo "Running ${BASH_SOURCE[0]}"

# work around for https://stackoverflow.com/a/47726910/458365
yes | sdkmanager "platforms;android-27"
