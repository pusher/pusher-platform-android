#!/usr/bin/env bash

openssl aes-256-cbc \
  -K $encrypted_764e7775f5b4_key \
  -iv $encrypted_764e7775f5b4_iv \
  -in $ROOT_DIR/gradle/firebase/secret.json.enc \
  -out $ROOT_DIR/gradle/firebase/secret.json -d

gcloud auth activate-service-account \
  --key-file $ROOT_DIR/gradle/firebase/secret.json