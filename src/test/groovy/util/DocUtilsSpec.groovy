package util

import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.StandardCopyOption

class DocUtilsSpec extends Specification {

    def "test extractZipArchive"() {
        given:
        def zipIs = getClass().getClassLoader().getResourceAsStream('templates.zip')
        def zipFile = Files.createTempFile('templates', '.zip')
        Files.copy(zipIs, zipFile, StandardCopyOption.REPLACE_EXISTING)
        def targetDir = Files.createTempDirectory('tst')
        def ret

        when: 'Target dir is empty and we extract the full archive'
        ret = DocUtils.extractZipArchive(zipFile, targetDir)

        then: 'Correctly extracted'
        ret == targetDir
        Files.isDirectory(ret)

        when: 'Target dir has content and we extract the full archive'
        ret = DocUtils.extractZipArchive(zipFile, targetDir)

        then: 'Correctly extracted'
        ret == targetDir
        Files.isDirectory(ret)

        when: 'Target dir has content and we extract part of the archive'
        ret = DocUtils.extractZipArchive(zipFile, targetDir, 'templates')

        then: 'Correctly extracted'
        ret == targetDir
        Files.isDirectory(ret)

        when: 'Target dir is empty and we extract part of the archive'
        targetDir.deleteDir()
        Files.createDirectory(targetDir)
        ret = DocUtils.extractZipArchive(zipFile, targetDir, 'templates')

        then: 'Correctly extracted'
        ret == targetDir
        Files.isDirectory(ret)

        when: 'Target dir does not exist and we extract part of the archive'
        targetDir.deleteDir()
        ret = DocUtils.extractZipArchive(zipFile, targetDir, 'templates')

        then: 'Correctly extracted'
        ret == targetDir
        Files.isDirectory(ret)

        when: 'Target dir does not exist and we extract the full archive'
        targetDir.deleteDir()
        ret = DocUtils.extractZipArchive(zipFile, targetDir, 'templates')

        then: 'Correctly extracted'
        ret == targetDir
        Files.isDirectory(ret)

        cleanup:
        targetDir.deleteDir()
    }

}
