package util

import java.nio.file.Path
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

import net.lingala.zip4j.core.ZipFile

class DocUtils {
  
  // Extract some Zip archive content into a target directory
  static def Path extractZipArchive(byte[] zipArchiveContent, Path targetDir) {
      def tmpFile = Files.createTempFile("archive-", ".zip")

      try {
          // Write content to a temp file
          Files.write(tmpFile, zipArchiveContent)

          // Create a ZipFile from the temp file
          ZipFile zipFile = new ZipFile(tmpFile.toFile())

          // Extract the ZipFile into targetDir
          zipFile.extractAll(targetDir.toString())
      } catch (Throwable e) {
          throw e
      } finally {
          Files.delete(tmpFile)
      }

      return targetDir
  }
}
