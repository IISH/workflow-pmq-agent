#!/bin/bash

set -e

export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
mvn clean package



version=$(git rev-parse master)
tag=$(git describe --tags)
name="registry.diginfra.net/lwo/pmq-agent"

docker build --tag="${name}:${tag}" .
docker tag "${name}:${tag}" "${name}:latest"
docker push "${name}:${tag}"
docker push "${name}:latest"

