#!/bin/bash

set -e

# Check params
if [ $# -ne 1 ]
	then
		echo Usage: $0 version;
    echo E.g: $0 0.1.0
		echo Version is MAJOR.MINOR.BUGFIX
		echo Latest versions:
		git tag | tail -n 5
		exit 1;
fi

# Set environment
export LANG="C.UTF-8"
export VERSION=$1

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

echo ----[ Upload docker image ]----
docker login
docker tag $DOCKER_IMAGE foilen/$DOCKER_IMAGE
docker tag $DOCKER_IMAGE foilen/redirectport-registry:latest
docker push foilen/$DOCKER_IMAGE
docker push foilen/redirectport-registry:latest

echo ----[ Git Tag ]==----
cd $RUN_PATH
git tag -a -m $VERSION $VERSION

echo ----[ Operation completed successfully ]==----

echo
echo You can see published items on https://hub.docker.com/r/foilen/redirectport-registry/tags/
echo You can send the tag: git push --tags
