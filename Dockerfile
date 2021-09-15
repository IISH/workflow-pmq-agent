# Pull base image.
FROM registry.diginfra.net/lwo/pmq-agent:v3.0.2
LABEL Description="PMQ agent" Version="3.0.12"

COPY target/pmq-agent-3.0.12.jar /home/jenkins/bin/pmq-agent.jar