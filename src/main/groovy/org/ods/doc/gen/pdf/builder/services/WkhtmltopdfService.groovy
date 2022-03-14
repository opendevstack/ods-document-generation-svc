package org.ods.doc.gen.pdf.builder.services

import groovy.util.logging.Slf4j
import org.apache.commons.io.output.TeeOutputStream
import org.springframework.stereotype.Service

import java.nio.file.Path
import java.nio.file.Paths

@Slf4j
@Service
class WkhtmltopdfService {

    void executeCmd(Path tmpDir, Path documentHtmlFile, List<String> cmd) {
        log.info "executing cmd: ${cmd}"

        def proc = cmd.execute()
        File tempFile  = Paths.get(tmpDir.toString(), "shell.bin").toFile()
        FileOutputStream tempFileOutputStream = new FileOutputStream(tempFile)
        def errOutputStream = new TeeOutputStream(tempFileOutputStream, System.err)
        try {
            proc.waitForProcessOutput(System.out, errOutputStream)
            if (proc.exitValue() != 0) {
                String errorDesc =   "${documentHtmlFile} failed: code:${proc.exitValue()}\r Description:${tempFile.text}"
                log.error errorDesc
                throw new IllegalStateException(errorDesc)
            }
        } finally {
            tempFileOutputStream.close()
        }

        log.info "executing cmd end"
    }

}
