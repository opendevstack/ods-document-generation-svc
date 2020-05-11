FROM adoptopenjdk/openjdk8:ubi-jre

MAINTAINER martin.etmajer@boehringer-ingelheim.com

WORKDIR /app
COPY app.jar ./app.jar

# Install wkhtmltopdf
RUN yum update -y && \
    yum install -y libX11 libXext libXrender libjpeg xz xorg-x11-fonts-Type1 git-core && \
    curl -kLO http://mirror.centos.org/centos/8/AppStream/aarch64/os/Packages/xorg-x11-fonts-75dpi-7.5-19.el8.noarch.rpm && \
    rpm -Uvh xorg-x11-fonts-75dpi-7.5-19.el8.noarch.rpm && \
    curl -kLO https://github.com/wkhtmltopdf/wkhtmltopdf/releases/download/0.12.5/wkhtmltox-0.12.5-1.centos8.x86_64.rpm && \
    rpm -Uvh wkhtmltox-0.12.5-1.centos8.x86_64.rpm

# See https://docs.openshift.com/container-platform/3.9/creating_images/guidelines.html
RUN chgrp -R 0 /app && \
    chmod -R g=u /app

USER 1001

EXPOSE 8080

CMD ["java", "-Xmx512m", "-jar", "app.jar"]
