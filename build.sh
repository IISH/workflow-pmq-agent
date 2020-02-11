#!/bin/bash

export JAVA_HOME=/usr/lib/jvm/jdk-9.0.1

mvn clean package
docker build --tag="registry.diginfra.net/lwo/pmq-agent:latest" .
docker push registry.diginfra.net/lwo/pmq-agent:latest
