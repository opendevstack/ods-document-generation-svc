package org.ods.doc.gen.templates.repository

import net.lingala.zip4j.ZipFile
import org.springframework.stereotype.Service
import org.springframework.util.FileSystemUtils

import java.nio.file.Files
import java.nio.file.Path

@Service
class ZipFacade {

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
        Files.write(tmpFile, zipArchiveContent)
        ZipFile zipFile = new ZipFile(tmpFile.toFile())
        zipFile
    }

}
