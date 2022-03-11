package org.ods.doc.gen.pdf.builder.util

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Service

import javax.inject.Inject

@Slf4j
@Service
class WkHtmlToPdfService {

    protected final String SERVICE_CMD_NAME = "wkhtmltopdf"

    @Inject
    private OSService OSService;

    List<String> getServiceCmd() {
        return [ SERVICE_CMD_NAME + OSService.getOSApplicationsExtension() ]
    }

}
