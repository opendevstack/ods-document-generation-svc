package org.ods.doc.gen.pdf.builder.util

import groovy.util.logging.Slf4j
import org.junit.platform.commons.util.StringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

@Slf4j
@Service
class CmdAdaptionsToRunInDockerContainer extends CmdToRunAdaptions {

    static final DockerImageName DOCGEN_IMAGE = DockerImageName.parse("ods-document-generation-svc:local");

    @Value('${runCmdsInDockerContainer}')
    String runCmdsInDockerContainer

    boolean enabled
    boolean startedServer

    GenericContainer<?> container

    CmdAdaptionsToRunInDockerContainer() {
        startedServer = false
        enabled = StringUtils.isNotBlank(runCmdsInDockerContainer) && "true".equalsIgnoreCase(runCmdsInDockerContainer)

        log.info("Using docker container to run cmds? ${enabled}")
    }

    List<String> modify(String cmd) {
        if (! enabled) {
            return [ cmd ]
        }

        startServer()

        String containerId = container.getContainerId()

        String [] cmdOut = [ : ]
        cmdOut.addAll(["docker", "exec", "-it", containerId ])
        cmdOut << cmd
        log.info("CMD: " + String.join(" ", cmdOut))
        return cmdOut
    }

    private startServer() {
        container = new GenericContainer<>(DOCGEN_IMAGE).withEnv("ROOT_LOG_LEVEL", "TRACE")
    }

}
