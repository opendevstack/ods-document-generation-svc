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
class WkHtmlToPdfInDockerService extends WkHtmlToPdfService {

    static final DockerImageName DOCGEN_IMAGE = DockerImageName.parse("ods-document-generation-svc:local");

    @Inject
    private OSService OSService;

    String runWkHtmlToPdfInDockerContainer

    boolean enabled
    boolean startedServer

    GenericContainer<?> container

    @Inject
    WkHtmlToPdfInDockerService(@Value('${runWkHtmlToPdfInDockerContainer}') String runWkHtmlToPdfInDockerContainer) {

        this.runWkHtmlToPdfInDockerContainer = runWkHtmlToPdfInDockerContainer
        enabled = StringUtils.isNotBlank(runWkHtmlToPdfInDockerContainer) && "true".equalsIgnoreCase(runWkHtmlToPdfInDockerContainer)
        startedServer = false
        log.info("Using docker container to run cmds (runWkHtmlToPdfInDockerContainer) ?  = ${enabled}")

    }

    List<String> getServiceCmd() {
        if (! enabled) {
            return [ SERVICE_CMD_NAME + OSService.getOSApplicationsExtension() ]
        }

        if (! startedServer) {
            startServer()
            startedServer = true
        }

        String containerId = container.getContainerId()

        // Does not work when allocate tty using -t
        ArrayList<String> cmdOut = []
        cmdOut.addAll([ getDockerDaemonServiceName(), "exec", "-i", containerId, SERVICE_CMD_NAME ])
        // log.info("CMD: " + String.join(" ", cmdOut))
        return cmdOut
    }

    private startServer() {
        container = new GenericContainer<>(DOCGEN_IMAGE)
                .withEnv("ROOT_LOG_LEVEL", "TRACE")
                .withFileSystemBind("/tmp", "/tmp")
        // In linux OS, all calls to wkhtmltopdf run from tmp to tmp.
        // In windows we have to
        log.info("Starting docker container in trace mode and binding of /tmp to /tmp")
        container.start()
        log.info("Docker container started without problems.")
    }

    private String getDockerDaemonServiceName() {
        return "docker" + OSService.getOSApplicationsExtension()
    }

}
