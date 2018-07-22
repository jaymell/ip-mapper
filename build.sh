#!/bin/bash -xe

[[ -z "$1" ]] && echo "Pass a tag" &&  exit 1
TAG="$1"

./gradlew build

pushd client && \
  npm install
  npm run build
popd

docker build -t $TAG .
