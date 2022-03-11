package org.ods.doc.gen.pdf.builder.util

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Service

import javax.inject.Inject

@Slf4j
@Service
class WkHtmlToPdfService {

    @Inject
    private OSService OSService;

    List<String> getServiceCmd() {
        return [ getServiceName() ]
    }

    private String getServiceName() {
        return getRawServiceName() + OSService.getOSApplicationsExtension();
    }

    protected String getRawServiceName() {
        return "wkhtmltopdf"
    }

}
