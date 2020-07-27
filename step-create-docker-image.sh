#!/bin/bash

set -e

RUN_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $RUN_PATH

echo ----[ Prepare folder for docker image ]----
DOCKER_BUILD=$RUN_PATH/build/docker

rm -rf $DOCKER_BUILD
mkdir -p $DOCKER_BUILD/app

cp -v build/libs/redirectport-registry-$VERSION-boot.jar $DOCKER_BUILD/app/redirectport-registry.jar
cp -v docker-release/* $DOCKER_BUILD
echo -n $VERSION > $DOCKER_BUILD/app/version.txt

echo ----[ Docker image folder content ]----
find $DOCKER_BUILD

echo ----[ Build docker image ]----
DOCKER_IMAGE=redirectport-registry:$VERSION
docker build -t $DOCKER_IMAGE $DOCKER_BUILD

rm -rf $DOCKER_BUILD
