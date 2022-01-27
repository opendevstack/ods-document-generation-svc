package org.ods.doc.gen.core

import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import org.ods.doc.gen.project.data.ProjectData
import org.springframework.stereotype.Service
import org.springframework.util.FileSystemUtils

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Service
class ZipFacade {

    static final String ARTIFACTS_BASE_DIR = 'artifacts'

    byte[] extractFromZipFile(String path, String fileToBeExtracted) {
        Path parentdir = Files.createDirectories(Paths.get(path).parent)

        def zipFile = new ZipFile(path)
        zipFile.extractFile(fileToBeExtracted, parentdir.toString())

        return new File(parentdir.toString(), fileToBeExtracted).getBytes()
    }

    byte[] createZipFileFromFiles(ProjectData projectData, String name, Map<String, byte[]> files) {
        def path = "${projectData.data.env.WORKSPACE}/${ARTIFACTS_BASE_DIR}/${name}".toString()
        Files.createDirectories(Paths.get(path).parent)

        def zipFile = new ZipFile(path)
        files.each { filePath, fileData ->
            def params = new ZipParameters()
            params.setFileNameInZip(filePath)
            zipFile.addStream(new ByteArrayInputStream(fileData), params)
        }

        return new File(path).getBytes()
    }

    void extractZipArchive(byte[] zipArchiveContent, Path targetDir) {
        def tmpFile = Files.createTempFile("archive-", ".zip")
        try {
            cleanTargetFolder(targetDir)
            createZipFile(tmpFile, zipArchiveContent).extractAll(targetDir.toString())
        } catch (Throwable e) {
            throw e
        } finally {
            Files.delete(tmpFile)
        }
    }

    private void cleanTargetFolder(Path targetDir) {
        FileSystemUtils.deleteRecursively(targetDir.toFile())
        targetDir.toFile().mkdirs()
    }

    private ZipFile createZipFile(Path tmpFile, byte[] zipArchiveContent) {
        Files.createDirectories(tmpFile.parent)
        Files.write(tmpFile, zipArchiveContent)
        return new ZipFile(tmpFile.toFile())
    }

}
