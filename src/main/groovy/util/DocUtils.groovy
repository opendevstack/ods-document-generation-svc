package util

import java.nio.file.Path
import java.nio.file.Files
import org.apache.commons.io.FileUtils

import net.lingala.zip4j.core.ZipFile

class DocUtils {
    static Exception tryDeleteThrowErrors(Path path, Throwable t) {
        Throwable thrown = tryDelete(path, t)
        if (thrown instanceof Error) {
            throw thrown
        }
        return (Exception) thrown
    }

    static Exception tryDeleteThrowErrors(File file, Throwable t) {
        Throwable thrown = tryDelete(file, t)
        if (thrown instanceof Error) {
            throw thrown
        }
        return (Exception) thrown
    }

    static Throwable tryDelete(Path path, Throwable t) {
        Throwable thrown = t
        try {
            Files.delete(path)
        } catch (Throwable suppressed) {
            thrown = processSecondThrowable(t, suppressed)
        }
        return thrown
    }

    static Throwable tryDelete(File file, Throwable t) {
        Throwable thrown = t
        try {
            file.delete()
        } catch (Throwable suppressed) {
            thrown = processSecondThrowable(t, suppressed)
        }
        return thrown
    }

    static Throwable processSecondThrowable(Throwable first, Throwable second) {
        Throwable t = first
        if (t == null) {
            t = second
        } else {
            Throwable suppressed = second
            if (suppressed != null) {
                if ((second instanceof Error) && (first instanceof Exception)) {
                    t = second
                    suppressed = first
                }
                t.addSuppressed(suppressed)
            }
        }
        return t
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
