package util

import net.lingala.zip4j.core.ZipFile
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.io.file.PathUtils

import java.nio.file.DirectoryNotEmptyException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class DocUtils {

    // Extract some Zip archive into a target directory
  static Path extractZipArchive(Path zipArchive, Path targetDir, String startAtDir = null) {
      // Create a ZipFile from the archive Path
      ZipFile zipFile = new ZipFile(zipArchive.toFile())

      if (startAtDir) {
          // Extract the ZipFile into targetDir from a given subDir
          def zipArchiveName = zipArchive.getFileName().toString()
          FileTools.withTempDir(FilenameUtils.removeExtension(zipArchiveName)) { tmpDir ->
              zipFile.extractAll(tmpDir.toString())
              def sourceDir = tmpDir.resolve(startAtDir)
              try {
                  Files.move(sourceDir, targetDir, StandardCopyOption.REPLACE_EXISTING)
              } catch (DirectoryNotEmptyException ignore) {
                  // Fallback in case targetDir is not in the same file system as sourceDir,
                  // or targetDir is not empty. Both cases should be rare.
                  PathUtils.copyDirectory(sourceDir, targetDir)
              }
          }
      } else {
          // Extract the ZipFile into targetDir from its root
          zipFile.extractAll(targetDir.toString())
      }
      return targetDir
  }

    static String getPDFHeader(Path pdf) {
        int len = -1
        def bytes = new byte[8]
        pdf.withInputStream { is ->
            len = IOUtils.read(is, bytes)
            if (len == bytes.length) {
                def b = is.read()
                // CR == 13, LF == 10. Both are valid here.
                if (b != 13 && b != 10) {
                    len = -len
                }
            }
        }
        if (len < bytes.length) {
            return null
        }
        def header = new String(bytes, 'ISO-8859-1')
        if (!header ==~ /%PDF-\d\.\d/) {
            return null
        }
        return header
    }
    
}
