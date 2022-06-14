#!/bin/bash

export JAVA_OPTS="${JAVA_OPTS} -Dagent.header.hostname=${HOSTNAME} -Dagent.header.pipeline=${ARCHIVEMATICA_HOST}"

/usr/local/openjdk-11/bin/java $JAVA_OPTS -server -jar pmq-agent-latest.jar
