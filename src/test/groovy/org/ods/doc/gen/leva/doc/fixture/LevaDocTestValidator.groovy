package org.ods.doc.gen.leva.doc.fixture

import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.ods.doc.gen.core.test.pdf.PdfCompare

@Slf4j
class LevaDocTestValidator {

    private static final boolean GENERATE_EXPECTED_PDF_FILES = Boolean.parseBoolean(System.properties["generateExpectedPdfFiles"] as String)

    static final String SAVED_DOCUMENTS = "build/reports/LeVADocs"

    private final File tempFolder

    LevaDocTestValidator(File tempFolder){
        this.tempFolder = tempFolder
    }

    boolean validatePDF(ProjectFixture projectFixture, String buildId) {
        unzipGeneratedArtifact(projectFixture, buildId)
        if (GENERATE_EXPECTED_PDF_FILES) {
            copyDocWhenRecording(projectFixture, buildId)
            return true
        } else {
            def actualFile = actualDoc(projectFixture, buildId)
            log.info("Validating pdf:${actualFile}")
            def expectedFile = expectedDoc(projectFixture, buildId)
            return new PdfCompare(SAVED_DOCUMENTS).compareAreEqual(actualFile.absolutePath, expectedFile.absolutePath)
        }
    }

    private void unzipGeneratedArtifact(projectFixture, String buildId) {
        String source = "${tempFolder.absolutePath}/artifacts/${getArtifactName(projectFixture, buildId)}.zip"
        String destination = "${tempFolder.absolutePath}"
        log.debug("unzipGeneratedArtifact src:[${source}]")
        log.debug("unzipGeneratedArtifact dest:[${destination}]")
        new AntBuilder().unzip(src: source, dest: destination, overwrite: "true")
    }

    private String getArtifactName(ProjectFixture projectFixture, buildId) {
        def comp =  (projectFixture.component) ? "${projectFixture.component}-" : ''
        def projectId = projectFixture.project
        return "${projectFixture.docType}-${ projectId.toUpperCase()}-${comp}${projectFixture.version}-${buildId}"
    }

    File expectedDoc(ProjectFixture projectFixture, String buildId) {
        def comp =  (projectFixture.component) ? "${projectFixture.component}/" : ''
        def filePath = "src/test/resources/expected/${projectFixture.project.toUpperCase()}/${comp}"
        new File(filePath).mkdirs()
        return new File("${filePath}/${getArtifactName(projectFixture, buildId)}.pdf")
    }

    private void copyDocWhenRecording(ProjectFixture projectFixture, String buildId) {
        FileUtils.copyFile(actualDoc(projectFixture, buildId), expectedDoc(projectFixture, buildId))
    }

    private File actualDoc(ProjectFixture projectFixture, String buildId) {
        return new File("${tempFolder.getAbsolutePath()}/${getArtifactName(projectFixture, buildId)}.pdf")
    }

}