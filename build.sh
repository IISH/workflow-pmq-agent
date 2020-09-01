#!/bin/bash

TAG_DEFAULT="latest"

tag="$1"
if [ -z "$tag" ]
then
	tag="$TAG_DEFAULT"
	echo "No tag. Set to ${tag}"
fi

export JAVA_HOME=/usr/lib/jvm/jdk-9.0.1

mvn clean package
docker build --tag="registry.diginfra.net/lwo/pmq-agent:${tag}" .
docker push "registry.diginfra.net/lwo/pmq-agent:${tag}"
