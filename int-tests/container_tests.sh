#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR/..

echo "building int test proxy server"
./gradlew :int-test-proxy-server:installDist
echo "building int test backend server"
./gradlew :int-test-backend-server:installDist

cd int-tests
docker-compose up --build
