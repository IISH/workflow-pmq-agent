# Pull base image.
FROM jfactory/java9-slave:latest
LABEL Description="PMQ agent" Version="3.0.11"

USER root

RUN curl -L "https://github.com/stedolan/jq/releases/download/jq-1.6/jq-linux64" -o "/home/jenkins/bin/jq" && \
    chmod 755 "/home/jenkins/bin/jq" && \
    /usr/bin/apt-get update && \
    /usr/bin/apt-get install -y mysql-client rsync p7zip-full tree imagemagick uuid python2-minimal && \
    chown -R jenkins:jenkins /home/jenkins && \
    ln -s /usr/bin/python2.7 /usr/bin/python && \
    groupadd -g 333 archivematica && useradd -u 333 -g 333 -M archivematica && usermod -a -G archivematica jenkins

COPY target/pmq-agent-3.0.0.jar /home/jenkins/bin/pmq-agent.jar

WORKDIR /home/jenkins

USER jenkins

ENV CONFIG_FILE=application.properties \
    MESSAGE_QUEUES=message_queues

CMD ["/usr/bin/java", "-server", "-jar", "/home/jenkins/bin/pmq-agent.jar"]
