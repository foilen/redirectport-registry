#!/bin/bash

set -e

# Set environment
export LANG="C.UTF-8"
export VERSION=$1

if [ -z "$VERSION" ]; then
	export VERSION=master-SNAPSHOT
fi

RUN_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $RUN_PATH

echo ----[ Update copyrights ]----
./scripts/javaheaderchanger.sh > /dev/null

echo ----[ Compile ]----
./gradlew build

echo ----[ Prepare folder for docker image ]----
DOCKER_BUILD=$RUN_PATH/build/docker

rm -rf $DOCKER_BUILD
mkdir -p $DOCKER_BUILD/app

cp -v build/distributions/redirectport-registry-$VERSION.zip $DOCKER_BUILD/app/redirectport-registry.zip
cp -v docker-release/* $DOCKER_BUILD
echo -n $VERSION > $DOCKER_BUILD/app/version.txt

cd $DOCKER_BUILD/app
unzip redirectport-registry.zip
rm redirectport-registry.zip
mv redirectport-registry-$VERSION/* .
rm -rf redirectport-registry-$VERSION

echo ----[ Docker image folder content ]----
find $DOCKER_BUILD

echo ----[ Build docker image ]----
DOCKER_IMAGE=redirectport-registry:$VERSION
docker build -t $DOCKER_IMAGE $DOCKER_BUILD

rm -rf $DOCKER_BUILD 
