#!/bin/bash -xe

TAG=$1

./gradlew build

pushd client && \
  npm install
  npm run build
popd

docker build -t ipmapper:$TAG .
