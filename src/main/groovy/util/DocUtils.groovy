package util

import java.nio.file.Path
import java.net.URI
import java.nio.file.Files
import java.io.File
import org.apache.commons.io.FileUtils

import net.lingala.zip4j.core.ZipFile

class DocUtils {
  
  // Extract some Zip archive content into a target directory
  static def Path extractZipArchive(byte[] zipArchiveContent, Path targetDir, String startAtDir = null) {
      def tmpFile = Files.createTempFile("archive-", ".zip")

      try {
          // Write content to a temp file
          Files.write(tmpFile, zipArchiveContent)

          // Create a ZipFile from the temp file
          ZipFile zipFile = new ZipFile(tmpFile.toFile())

          // Extract the ZipFile into targetDir - either from its root, or from a given subDir
          zipFile.extractAll(targetDir.toString())

          if (startAtDir) {
             FileUtils.copyDirectory(new File(targetDir.toFile(), startAtDir), targetDir.toFile())
             FileUtils.deleteDirectory(new File(targetDir.toFile(), startAtDir))
          }
      } catch (Throwable e) {
          throw e
      } finally {
          Files.delete(tmpFile)
      }

      return targetDir
  }
}
