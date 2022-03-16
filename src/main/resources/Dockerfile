FROM vindevoy/centos8-openjdk11

# Install wkhtmltopdf
RUN cd /etc/yum.repos.d/ && \
    sed -i 's/mirrorlist/#mirrorlist/g' /etc/yum.repos.d/CentOS-*  && \
    sed -i 's|#baseurl=http://mirror.centos.org|baseurl=http://vault.centos.org|g' /etc/yum.repos.d/CentOS-*  && \
    yum install -y libX11 libXext libXrender libjpeg xz xorg-x11-fonts-Type1 git-core  && \
    curl -kLO http://repo.okay.com.mx/centos/8/x86_64/release/xorg-x11-fonts-75dpi-7.5-19.el8.noarch.rpm  && \
    rpm -Uvh xorg-x11-fonts-75dpi-7.5-19.el8.noarch.rpm  && \
    curl -kLO https://github.com/wkhtmltopdf/packaging/releases/download/0.12.6-1/wkhtmltox-0.12.6-1.centos8.x86_64.rpm && \
    rpm -Uvh wkhtmltox-0.12.6-1.centos8.x86_64.rpm && \
    useradd pdfuser

USER pdfuser