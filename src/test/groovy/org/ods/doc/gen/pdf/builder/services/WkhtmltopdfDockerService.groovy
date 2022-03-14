package org.ods.doc.gen.core.test.pdf

import groovy.util.logging.Slf4j
import org.ods.doc.gen.pdf.builder.services.WkhtmltopdfService
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.startupcheck.IndefiniteWaitOneShotStartupCheckStrategy
import org.testcontainers.utility.DockerImageName

import java.nio.file.Path

@Slf4j
@Service
@Profile("test")
class WkhtmltopdfDockerService extends WkhtmltopdfService {

    static final DockerImageName DOCGEN_IMAGE = DockerImageName.parse("docgen-base:latest")

    void executeCmd(Path tmpDir, Path documentHtmlFile, List<String> cmd) {
        log.info "executing cmd: ${cmd}"

        new GenericContainer<>(DOCGEN_IMAGE)
                .withFileSystemBind(tmpDir.toString(), tmpDir.toString(), BindMode.READ_WRITE)
                .withLogConsumer(new Slf4jLogConsumer(log))
                .withCommand(cmd as String[])
                .withStartupCheckStrategy(
                        new IndefiniteWaitOneShotStartupCheckStrategy()
                ).start()

        log.info "executing cmd end"
    }

}
