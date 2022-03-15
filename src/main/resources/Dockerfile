FROM vindevoy/centos8-openjdk11

ARG HTTP_PROXY=""

COPY yum.repos.d/centos8.repo /etc/yum.repos.d/centos8.repo
COPY install_wkhtmltopdf.sh /root/
RUN chmod +x /root/install_wkhtmltopdf.sh && \
    /root/install_wkhtmltopdf.sh ${HTTP_PROXY} && \
    useradd pdfuser

USER pdfuser