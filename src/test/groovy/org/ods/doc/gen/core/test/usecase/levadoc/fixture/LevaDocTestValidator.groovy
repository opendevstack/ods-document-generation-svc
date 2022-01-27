package org.ods.doc.gen.core.test.usecase.levadoc.fixture

import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.ods.doc.gen.core.test.pdf.PdfCompare
import org.ods.doc.gen.project.data.Project

@Slf4j
class LevaDocTestValidator {

    static final String SAVED_DOCUMENTS = "build/reports/LeVADocs"

    private final File tempFolder
    private final String simpleClassName
    private final Project project

    LevaDocTestValidator(String simpleClassName,
                         File tempFolder,
                         Project project){
        this.simpleClassName = simpleClassName
        this.tempFolder = tempFolder
        this.project = project
    }

    boolean validatePDF(Boolean generateExpectedFiles, ProjectFixture projectFixture) {
        unzipGeneratedArtifact(projectFixture)
        if (generateExpectedFiles) {
            copyDocWhenRecording(projectFixture)
            return true
        } else {
            def actualFile = actualDoc(projectFixture)
            def expectedFile = expectedDoc(projectFixture)
            return new PdfCompare(SAVED_DOCUMENTS).compareAreEqual(actualFile.absolutePath, expectedFile.absolutePath)
        }
    }

    private void unzipGeneratedArtifact(projectFixture) {
        new AntBuilder().unzip(
            src: "${tempFolder.absolutePath}/artifacts/${getArtifactName(projectFixture)}.zip",
            dest: "${tempFolder.absolutePath}",
            overwrite: "true")
    }

    private String getArtifactName(ProjectFixture projectFixture) {
        def comp =  (projectFixture.component) ? "${projectFixture.component}-" : ''
        def projectId = projectFixture.project
        return "${projectFixture.docType}-${ projectId}-${comp}${projectFixture.version}-1"
    }

    private void copyDocWhenRecording(ProjectFixture projectFixture) {
        FileUtils.copyFile(actualDoc(projectFixture), expectedDoc(projectFixture))
    }

    private File actualDoc(ProjectFixture projectFixture) {
        return new File("${tempFolder.getAbsolutePath()}/${getArtifactName(projectFixture)}.pdf")
    }

    File expectedDoc(ProjectFixture projectFixture) {
        return new LevaDocDataFixture(simpleClassName, tempFolder, project).expectedDoc(projectFixture)
    }

}
