FROM openjdk:11
LABEL Description="PMQ agent" Version="3.0.20"

USER root

RUN groupadd -g 1000 linuxadmin && useradd -u 1000 -g 1000 -M linuxadmin && usermod -a -G linuxadmin linuxadmin && \
    groupadd -g 333 archivematica && useradd -u 333 -g 333 -M archivematica && usermod -a -G archivematica archivematica && \
    curl -L 'https://dl.min.io/client/mc/release/linux-amd64/mc' -o '/usr/bin/mc' && \
    curl -L 'https://github.com/stedolan/jq/releases/download/jq-1.6/jq-linux64' -o '/usr/bin/jq' && \
    chmod 775 '/usr/bin/jq' '/usr/bin/mc'  && \
    /usr/bin/apt-get update && \
    /usr/bin/apt-get install -y mariadb-client rsync p7zip-full tree imagemagick ffmpeg uuid python2-minimal && \
    ln -s /usr/bin/python2.7 /usr/bin/python

WORKDIR /home/linuxadmin

USER linuxadmin

COPY target/pmq-agent-* /home/linuxadmin/pmq-agent-latest.jar
COPY entrypoint.sh /home/linuxadmin/entrypoint.sh

ENV CONFIG_FILE=application.properties \
    MESSAGE_QUEUES=message_queues \
    JAVA_OPTS="" \
    ARCHIVEMATICA_HOST="http://localhost"

CMD ["/home/linuxadmin/entrypoint.sh"]
