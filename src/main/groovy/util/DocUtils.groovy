package util

import java.nio.file.Path
import java.nio.file.Files
import org.apache.commons.io.FileUtils

import net.lingala.zip4j.core.ZipFile

class DocUtils {
    static Exception tryDelete(Path path, Throwable t = null) {
        Exception suppressed = null
        try {
            Files.delete(path)
        } catch (Error e) {
            processError(t, e)
        } catch (Exception e) {
            processSuppressed(t, e)
            suppressed = e
        }
        if (t != null) {
            throw t
        }
        return suppressed
    }

    static Exception tryDelete(File file, Throwable t = null) {
        Exception suppressed = null
        try {
            file.delete()
        } catch (Error e) {
            processError(t, e)
        } catch (Exception e) {
            processSuppressed(t, e)
            suppressed = e
        }
        if (t != null) {
            throw t
        }
        return suppressed
    }

    private static void processError(Throwable t, Error suppressed) {
        if (t == null) {
            throw suppressed
        }
        if (t instanceof Exception) {
            suppressed.addSuppressed(t)
            throw suppressed
        }
        processSuppressed(t, suppressed)
    }

    private static void processSuppressed(Throwable t, Throwable suppressed) {
        if (t != null) {
            t.addSuppressed(suppressed)
        }
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
