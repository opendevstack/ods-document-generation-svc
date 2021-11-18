FROM vindevoy/centos8-openjdk11

# Install wkhtmltopdf
RUN yum update -y && \
    yum install -y libX11 libXext libXrender libjpeg xz xorg-x11-fonts-Type1 git-core && \
    curl -kLO http://mirror.centos.org/centos/8/AppStream/aarch64/os/Packages/xorg-x11-fonts-75dpi-7.5-19.el8.noarch.rpm && \
    rpm -Uvh xorg-x11-fonts-75dpi-7.5-19.el8.noarch.rpm && \
    curl -kLO https://github.com/wkhtmltopdf/packaging/releases/download/0.12.6-1/wkhtmltox-0.12.6-1.centos8.x86_64.rpm && \
    rpm -Uvh wkhtmltox-0.12.6-1.centos8.x86_64.rpm

