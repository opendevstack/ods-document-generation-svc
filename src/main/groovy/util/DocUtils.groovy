package util

import java.nio.file.Path
import java.nio.file.Files
import org.apache.commons.io.FileUtils

import net.lingala.zip4j.core.ZipFile

class DocUtils {
    static Exception tryDeleteThrowErrors(Path path, Throwable t) {
        t = t ? tryDelete(path, t) : tryDelete(path)
        if (t instanceof Error) {
            throw t
        }
        return (Exception) t
    }

    static Exception tryDeleteThrowErrors(File file, Throwable t) {
        t = t ? tryDelete(file, t) : tryDelete(file)
        if (t instanceof Error) {
            throw t
        }
        return (Exception) t
    }

    static Throwable tryDelete(Path path, Throwable t) {
        Throwable suppressed = tryDelete(path)
        Throwable thrown = processSecondThrowable(t, suppressed)
        return thrown
    }

    static Throwable tryDelete(File file, Throwable t) {
        Throwable suppressed = tryDelete(file)
        Throwable thrown = processSecondThrowable(t, suppressed)
        return thrown
    }

    static Throwable tryDelete(Path path) {
        Throwable thrown = null
        try {
            Files.delete(path)
        } catch (Throwable t) {
            try {
                path.toFile().deleteOnExit()
            } catch (Throwable suppressed) {
                t = processSecondThrowable(t, suppressed)
            }
            thrown = t
        }
        return thrown
    }

    static Throwable tryDelete(File file) {
        Throwable thrown = null
        try {
            file.delete()
        } catch (Throwable t) {
            try {
                file.deleteOnExit()
            } catch (Throwable suppressed) {
                t = processSecondThrowable(t, suppressed)
            }
            thrown = t
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
