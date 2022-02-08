package org.ods.doc.gen.pdf.builder.util

import org.springframework.stereotype.Service

@Service
class OSService {

    private String getOSName() {
        System.getProperty("os.name");
    }

    private boolean isWindows() {
        this.getOSName().startsWith("Windows")
    }

    String getOSApplicationsExtension() {
        this.isWindows() ? ".exe" : ""
    }
}
