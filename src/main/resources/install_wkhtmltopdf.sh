#!/bin/sh

ME=$(basename $0)

# NOTE: We use proxy when running gradle in local environment.
if [ ! -z "${HTTP_PROXY}" ] && [ "" != "${HTTP_PROXY}" ]; then
    echo -n "${ME}: Proxy for yum: "
    echo "proxy=${HTTP_PROXY}" | tee -a /etc/yum.conf
    echo " "
    echo -n "${ME}: Proxy for curl: "
    echo "proxy = \"${HTTP_PROXY}\"" | tee -a /root/.curlrc
    echo " "
fi

echo "${ME}: Fixing yum repos list with sed..."
sed -i 's/mirrorlist/#mirrorlist/g' /etc/yum.repos.d/CentOS-*
sed -i 's|#baseurl=http://mirror.centos.org|baseurl=http://vault.centos.org|g' /etc/yum.repos.d/CentOS-*

echo "${ME}: Installing dependencies..."
yum install -y libX11 libXext libXrender libjpeg xz xorg-x11-fonts-Type1 git-core xorg-x11-fonts-75dpi

# Install wkhtmltopdf
cd /tmp
curl -kLO https://github.com/wkhtmltopdf/packaging/releases/download/0.12.6-1/wkhtmltox-0.12.6-1.centos8.x86_64.rpm
rpm -Uvh wkhtmltox-0.12.6-1.centos8.x86_64.rpm

yum clean all

find /usr/share/doc -depth -type f ! -name copyright | xargs rm || true
find /usr/share/doc -empty | xargs rmdir || true
rm -rf /usr/share/groff/* /usr/share/info/*
rm -rf /usr/share/lintian/* /usr/share/linda/* /var/cache/man/*
