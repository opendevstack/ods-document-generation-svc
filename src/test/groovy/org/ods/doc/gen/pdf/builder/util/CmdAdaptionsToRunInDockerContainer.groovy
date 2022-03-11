package org.ods.doc.gen.pdf.builder.util

import groovy.util.logging.Slf4j
import org.junit.platform.commons.util.StringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

import javax.inject.Inject

@Slf4j
@Service
class CmdAdaptionsToRunInDockerContainer extends CmdToRunAdaptions {

    static final DockerImageName DOCGEN_IMAGE = DockerImageName.parse("ods-document-generation-svc:local");

    String runCmdsInDockerContainer

    boolean enabled
    boolean startedServer

    GenericContainer<?> container

    @Inject
    CmdAdaptionsToRunInDockerContainer(@Value('${runCmdsInDockerContainer}') String runCmdsInDockerContainer) {
        this.runCmdsInDockerContainer = runCmdsInDockerContainer
        enabled = StringUtils.isNotBlank(runCmdsInDockerContainer) && "true".equalsIgnoreCase(runCmdsInDockerContainer)
        startedServer = false
        log.info("Using docker container to run cmds? runCmdsInDockerContainer = ${enabled}")
    }

    List<String> modify(String cmd) {
        if (! enabled) {
            return [ cmd ]
        }

        startServer()

        String containerId = container.getContainerId()

        // Does not work when allocate tty using -t
        String [] cmdOut = ["docker", "exec", "-i", containerId, cmd ]
        log.info("CMD: " + String.join(" ", cmdOut))
        return cmdOut
    }

    private startServer() {
        container = new GenericContainer<>(DOCGEN_IMAGE)
                .withEnv("ROOT_LOG_LEVEL", "TRACE")
                .withFileSystemBind("/tmp", "/tmp")
        container.start()
    }

}
