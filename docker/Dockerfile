FROM registry.access.redhat.com/rhel7

MAINTAINER martin.etmajer@boehringer-ingelheim.com

WORKDIR /app
COPY app.jar ./app.jar

# Install wkhtmltopdf
RUN yum install -y libX11 libXext libXrender libjpeg xz xorg-x11-fonts-75dpi xorg-x11-fonts-Type1 openssl git-core fontconfig && \
    curl -kLO https://github.com/wkhtmltopdf/wkhtmltopdf/releases/download/0.12.5/wkhtmltox-0.12.5-1.centos7.x86_64.rpm && \
    rpm -Uvh wkhtmltox-0.12.5-1.centos7.x86_64.rpm

RUN yum -y install java-1.8.0-openjdk-devel.x86_64

# See https://docs.openshift.com/container-platform/3.9/creating_images/guidelines.html
RUN chgrp -R 0 /app && \
    chmod -R g=u /app

USER 1001

EXPOSE 8080

CMD ["java", "-Xmx512m", "-jar", "app.jar"]
