FROM adoptopenjdk/openjdk8:ubi-jre

MAINTAINER martin.etmajer@boehringer-ingelheim.com

WORKDIR /app
COPY app.jar ./app.jar
COPY entrypoint.sh ./entrypoint.sh

RUN yum update -y && \
    yum install -y libX11 libXext libXrender libjpeg xz xorg-x11-fonts-Type1 git-core

# Install wkhtmltopdf
COPY yum.repos.d/centos8.repo /etc/yum.repos.d/centos8.repo
RUN yum install -y xorg-x11-fonts-75dpi && \
    curl -kLO https://github.com/wkhtmltopdf/wkhtmltopdf/releases/download/0.12.5/wkhtmltox-0.12.5-1.centos8.x86_64.rpm && \
    rpm -Uvh wkhtmltox-0.12.5-1.centos8.x86_64.rpm && chmod +x entrypoint.sh

# See https://docs.openshift.com/container-platform/3.9/creating_images/guidelines.html
RUN chgrp -R 0 /app && \
    chmod -R g=u /app

USER 1001

EXPOSE 8080
ENV JAVA_MEM_XMX="512m" \
    JAVA_MEM_XMS="128m" \
    JAVA_OPTS="-XX:+UseCompressedOops -XX:+UseG1GC -XX:+UseStringDeduplication -XX:MaxGCPauseMillis=1000"

ENTRYPOINT /app/entrypoint.sh
