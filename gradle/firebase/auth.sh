#!/usr/bin/env bash

echo "Running ${BASH_SOURCE[0]}"

openssl aes-256-cbc \
  -K $encrypted_764e7775f5b4_key \
  -iv $encrypted_764e7775f5b4_iv \
  -in ./gradle/firebase/secret.json.enc \
  -out ./gradle/firebase/secret.json -d

gcloud auth activate-service-account \
  --key-file ./gradle/firebase/secret.json