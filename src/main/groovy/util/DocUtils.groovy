package util

import java.nio.file.Path
import java.nio.file.Files
import org.apache.commons.io.FileUtils

import net.lingala.zip4j.core.ZipFile

class DocUtils {
    /**
     * Tries to delete the file given by <code>path</code>.
     * If file deletion throws an <code>Exception</code> this exception is suppressed and execution continues normally.
     * If an <code>Error</code> is thrown, it will however be propagated.
     * If the optional <code>Throwable t</code> is given, it will be thrown and the method will never return.
     * If <code>t</code> is an <code>Exception</code> and file deletion throws an <code>Error</code>,
     * then <code>t</code> will be suppressed and the <code>Error</code> will be thrown instead.
     *
     * The <code>t</code> parameter is typically used when trying to delete a file inside a <code>catch</code> block.
     * <pre>
     *     Typical usage:
     *     {@code
     *     def path = Files.crateTempFile('prefix', null)
     *     try {
     *         // Do something with the temp file
     *     } catch (Throwable t) {
     *         DocUtils.tryDelete(path, t) // Never returns
     *     }
     *     def suppressed = DocUtils.tryDelete(path) // Only reached, if no Throwable was caught
     *     // Execution continues normally, even if file deletion failed with an Exception.
     *     // The suppressed exception is available, for example, to log a warning or to be rethrown, if desired.
     *     }
     * </pre>
     *
     * @param path <code>Path</code> to the file to try to delete.
     * @param t optional <code>Throwable</code> to be thrown after trying to delete the file.
     * @return the suppressed <code>Exception</code> thrown by the file deletion, if any.
     * @throws Error if <code>t</code> is an <code>Error</code> or an <code>Error</code> is thrown by the file deletion.
     * @throws Exception if <code>t</code> is an <code>Exception</code>.
     */
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

    /**
     * Tries to delete the <code>File</code> given by <code>file</code>.
     * If file deletion throws an <code>Exception</code> this exception is suppressed and execution continues normally.
     * If an <code>Error</code> is thrown, it will however be propagated.
     * If the optional <code>Throwable t</code> is given, it will be thrown and the method will never return.
     * If <code>t</code> is an <code>Exception</code> and file deletion throws an <code>Error</code>,
     * then <code>t</code> will be suppressed and the <code>Error</code> will be thrown instead.
     *
     * The <code>t</code> parameter is typically used when trying to delete a file inside a <code>catch</code> block.
     * <pre>
     *     Typical usage:
     *     {@code
     *     def file = File.crateTempFile('prefix', null)
     *     try {
     *         // Do something with the temp file
     *     } catch (Throwable t) {
     *         DocUtils.tryDelete(file, t) // Never returns
     *     }
     *     def suppressed = DocUtils.tryDelete(file) // Only reached, if no Throwable was caught
     *     // Execution continues normally, even if file deletion failed with an Exception.
     *     // The suppressed exception is available, for example, to log a warning or to be rethrown, if desired.
     *     }
     * </pre>
     *
     * @param file <code>File</code> to try to delete.
     * @param t optional <code>Throwable</code> to be thrown after trying to delete the file.
     * @return the suppressed <code>Exception</code> thrown by the file deletion, if any.
     * @throws Error if <code>t</code> is an <code>Error</code> or an <code>Error</code> is thrown by the file deletion.
     * @throws Exception if <code>t</code> is an <code>Exception</code>.
     */
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

    private static void processError(Throwable t, Error e) {
        if (t == null) {
            throw e
        }
        if (t instanceof Exception) {
            e.addSuppressed(t)
            throw e
        }
        processSuppressed(t, e)
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
