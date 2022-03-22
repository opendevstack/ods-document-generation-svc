package org.ods.doc.gen.pdf.builder.services

import groovy.util.logging.Slf4j
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

    static final String IMAGE__BASE = "jdk-11_openj9-wkhtmltopdf-ubi:local"
    static final DockerImageName DOCGEN_IMAGE = DockerImageName.parse(IMAGE__BASE)
    static final String TMP_PDF = "/tmp/pdf"
    public static final String WINDOWS_MNT = "/mnt/c/"
    public static final String WINDOWS_C = "C:\\"

    void executeCmd(Path tmpDir, Path documentHtmlFile, List<String> cmd) {
        log.info "executing cmd: ${cmd}"

        String[] cmdInDocker = useDockerPaths(cmd, tmpDir.toString())
        log.info "cmdInDocker: ${cmdInDocker}"

        String pathToFiles = replaceWindowsPath(tmpDir)
        log.info "pathToFiles: ${pathToFiles}"

        new GenericContainer<>(DOCGEN_IMAGE)
                .withFileSystemBind(pathToFiles, TMP_PDF, BindMode.READ_WRITE)
                .withLogConsumer(new Slf4jLogConsumer(log))
                .withCommand(cmdInDocker)
                .withStartupCheckStrategy(
                        new IndefiniteWaitOneShotStartupCheckStrategy()
                ).start()

        log.info "executing cmd end"
    }

    private String replaceWindowsPath(Path tmpDir) {
        return tmpDir.toString().replace(WINDOWS_C, WINDOWS_MNT).replace("\\", "/")
    }

    private String[] useDockerPaths(List<String> cmd, String tmpDirPath) {
        def cmdLinux = []
        cmd.forEach { it ->
            if (it.contains(tmpDirPath)) {
                cmdLinux << it.replace(tmpDirPath, TMP_PDF).replace("\\", "/")
            } else {
                cmdLinux << it
            }
        }
        return cmdLinux
    }

}
