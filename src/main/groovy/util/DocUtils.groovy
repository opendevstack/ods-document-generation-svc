package util

import java.nio.file.Path
import java.nio.file.Files
import org.apache.commons.io.FileUtils

import net.lingala.zip4j.core.ZipFile

class DocUtils {
    static void tryDeleteAndRethrow(Path path, Throwable t) {
        Throwable suppressed
        try {
            suppressed = tryDelete(path)
        } catch (Error e) {
            if (t instanceof Error) {
                suppressed = e
            } else {
                e.addSuppressed(t)
                throw e
            }
        }
        if (suppressed) {
            t.addSuppressed(suppressed)
        }
        throw t
    }

    static Exception tryDelete(Path path) {
        try {
            Files.delete(path)
        } catch (Throwable t) {
            try {
                path.toFile().deleteOnExit()
            } catch (Exception suppressed) {
                t.addSuppressed(suppressed)
            }
            if (t instanceof Error) {
                throw t
            }
            return (Exception) t
        }
        return null
    }

    static Exception tryDelete(File file) {
        try {
            file.delete()
        } catch (Throwable t) {
            try {
                file.deleteOnExit()
            } catch (Exception suppressed) {
                t.addSuppressed(suppressed)
            }
            if (t instanceof Error) {
                throw t
            }
            return (Exception) t
        }
        return null
    }

    // Extract some Zip archive content into a target directory
  static Path extractZipArchive(byte[] zipArchiveContent, Path targetDir, String startAtDir = null) {
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
