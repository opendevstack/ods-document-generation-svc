package org.ods.shared.lib.jenkins

import groovy.util.logging.Slf4j
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import org.ods.shared.lib.project.data.Project
import org.ods.shared.lib.git.GitService
import org.ods.shared.lib.project.data.ProjectData
import org.springframework.stereotype.Service

import javax.inject.Inject

@SuppressWarnings(['JavaIoPackageAccess', 'PublicMethodsBeforeNonPublicMethods'])
@Slf4j
@Service
class PipelineUtil {

    static final String ARTIFACTS_BASE_DIR = 'artifacts'
    static final String LOGS_BASE_DIR = 'logs'
    static final String SONARQUBE_BASE_DIR = 'sonarqube'
    static final String XUNIT_DOCUMENTS_BASE_DIR = 'xunit'

    protected Project project
    protected GitService git

    @Inject
    PipelineUtil(Project project, GitService git) {
        this.project = project
        this.git = git
    }
    
    protected File createDirectory(String path) {
        if (!path?.trim()) {
            throw new IllegalArgumentException("Error: unable to create directory. 'path' is undefined.")
        }

        def dir = new File(path)
        dir.mkdirs()
        return dir
    }

    byte[] createZipArtifact(ProjectData projectData, String name, Map<String, byte[]> files, boolean doCreateArtifact = true) {
        if (!name?.trim()) {
            throw new IllegalArgumentException("Error: unable to create Zip artifact. 'name' is undefined.")
        }

        if (files == null) {
            throw new IllegalArgumentException("Error: unable to create Zip artifact. 'files' is undefined.")
        }

        def path = "${projectData.data.env.WORKSPACE}/${ARTIFACTS_BASE_DIR}/${name}".toString()
        def result = this.createZipFile(path, files)
        if (doCreateArtifact) {
          //  this.archiveArtifact(path, result) // TODO s2o
        }

        return result
    }

    void createAndStashArtifact(ProjectData projectData, String stashName, byte[] file) {
        if (!stashName?.trim()) {
            throw new IllegalArgumentException("Error: unable to stash artifact. 'stashName' is undefined.")
        }

        if (file == null) {
            throw new IllegalArgumentException("Error: unable to stash artifact. 'file' is undefined.")
        }

        def path = "${projectData.data.env.WORKSPACE}/${ARTIFACTS_BASE_DIR}/${stashName}".toString()

        // Create parent directory if needed
        this.createDirectory(new File(path).getParent())
        new File(path) << (file)

        // TODO s2o stash
      /*  this.steps.dir(new File(path).getParent()) {
            this.steps.stash(['name': stashName, 'includes': stashName])
        }*/
    }

    
    byte[] createZipFile(String path, Map<String, byte[]> files) {
        if (!path?.trim()) {
            throw new IllegalArgumentException("Error: unable to create Zip file. 'path' is undefined.")
        }

        if (files == null) {
            throw new IllegalArgumentException("Error: unable to create Zip file. 'files' is undefined.")
        }

        // Create parent directory if needed
        this.createDirectory(new File(path).getParent())

        // Create the Zip file
        def zipFile = new ZipFile(path)
        files.each { filePath, fileData ->
            def params = new ZipParameters()
            params.setFileNameInZip(filePath)
            zipFile.addStream(new ByteArrayInputStream(fileData), params)
        }

        return new File(path).getBytes()
    }

    
    byte[] extractFromZipFile(String path, String fileToBeExtracted) {
        // Create parent directory if needed
        File parentdir = this.createDirectory(new File(path).getParent())

        // Create the Zip file
        def zipFile = new ZipFile(path)
        zipFile.extractFile(fileToBeExtracted, parentdir.getAbsolutePath())

        return new File(parentdir, fileToBeExtracted).getBytes()
    }

    void warnBuild(String message) {
        this.steps.currentBuild.result = 'UNSTABLE'
        this.log.warn(message)
    }

}
