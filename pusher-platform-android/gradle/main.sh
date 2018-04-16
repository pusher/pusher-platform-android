#!/usr/bin/env bash

./gradlew \
  :library:build \
  :library:connectedAndroidTest \
  :library:codeCoverageReport