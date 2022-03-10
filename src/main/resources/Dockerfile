FROM vindevoy/centos8-openjdk11
ARG HTTP_PROXY="http://appaccess-zscaler.boehringer.com:80"

COPY yum.repos.d/centos8.repo /etc/yum.repos.d/centos8.repo
# Install wkhtmltopdf dependencies
RUN echo "proxy=${HTTP_PROXY}" | tee -a /etc/yum.conf && \
    cd /etc/yum.repos.d/ && \
    sed -i 's/mirrorlist/#mirrorlist/g' /etc/yum.repos.d/CentOS-*  && \
    sed -i 's|#baseurl=http://mirror.centos.org|baseurl=http://vault.centos.org|g' /etc/yum.repos.d/CentOS-*  && \
    yum install -y libX11 libXext libXrender libjpeg xz xorg-x11-fonts-Type1 git-core xorg-x11-fonts-75dpi

# Install wkhtmltopdf
RUN echo "proxy = \"${HTTP_PROXY}\"" | tee -a /root/.curlrc && \
    curl -kLO https://github.com/wkhtmltopdf/packaging/releases/download/0.12.6-1/wkhtmltox-0.12.6-1.centos8.x86_64.rpm && \
    rpm -Uvh wkhtmltox-0.12.6-1.centos8.x86_64.rpm
