#!/usr/bin/env bash

echo "Running ${BASH_SOURCE[0]}"

echo "Version before install:"
gcloud version || true
if [ ! -d "$HOME/google-cloud-sdk/bin" ]; then
  echo "Installing gcloud"
  rm -rf $HOME/google-cloud-sdk;
  export CLOUDSDK_CORE_DISABLE_PROMPTS=1;
  curl https://sdk.cloud.google.com | bash;
  ./gradle/firebase/auth.sh
  gcloud init --console-only
else
  echo "Found 'gcloud' installed"
fi
source $HOME/google-cloud-sdk/path.bash.inc
echo "Version after install"
gcloud version
echo "Update gcloud components"
gcloud --quiet components update
echo "List components: "
gcloud components list
