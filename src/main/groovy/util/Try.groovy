package util

import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FirstParam
import org.apache.commons.io.FileUtils
import org.apache.commons.io.file.PathUtils

import java.nio.file.Files
import java.nio.file.Path

/**
 * Extension class implementing functionality equivalent to withCloseable, but generalised to resources
 * that do not implement {@code AutoCloseable} by providing an optional cleanup closure.
 * If no cleanup closure is given and no specialised method exists for the given resource class,
 * by default it will try to invoke the {@code close()} method.
 *
 * An additional functionality is also supported to initialise new resources and to only run the cleanup
 * code in case of exception while performing the construction.
 *
 * Specialised convenience methods are also included to work with {@code File} or {@code Path}
 * and have them deleted when the cleanup is invoked.
 */
class Try {

    private static final Closure<?> deleteFile = { File file -> Files.deleteIfExists(file.toPath()) }
    private static final Closure<?> deletePath = { Path path -> Files.deleteIfExists(path) }
    private static final Closure<?> deleteDir = { File dir ->
        dir.toPath() // Validate path.
        FileUtils.deleteDirectory(dir)
    }
    private static final Closure<?> deleteDirContents = { File dir ->
        if (Files.exists(dir.toPath())) {
            FileUtils.cleanDirectory(dir)
        }
        return null
    }
    private static final Closure<?> deleteDirPath = { Path dir ->
        if (Files.notExists(dir)) {
            return null
        }
        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException("Not a directory: ${dir}")
        }
        PathUtils.deleteDirectory(dir)
    }
    private static final Closure<?> deleteDirContentsPath = { Path dir ->
        if (Files.notExists(dir)) {
            return null
        }
        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException("Not a directory: ${dir}")
        }
        PathUtils.cleanDirectory(dir)
    }

    private Try() {}

    /**
     * Passes this Path to the closure, ensuring that the file is deleted after the closure returns,
     * regardless of errors.
     *
     * As with the try-with-resources statement, if multiple exceptions are thrown, the exception from the closure
     * will be returned and the exception from deleting will be added as a suppressed exception.
     *
     * Usage example:
     *
     * {@Code
     * Files.createTempFile('tst', null).withFile { tmpFile ->
     *     // Do something with your temp file.
     *     // If any exception is thrown, the file will be deleted.
     * }
     * // The file has been deleted
     * }
     *
     * @param self  the {@code Path}.
     * @param block the closure taking the {@code Path} as parameter.
     * @return      the value returned by the closure.
     * @throws java.nio.file.DirectoryNotEmptyException
     *         if the file is a directory and could not otherwise be deleted
     *         because the directory is not empty <i>(optional specific
     *         exception)</i>
     * @throws IOException
     *         if an I/O error occurs while deleting the file.
     * @throws SecurityException
     *         In the case of the default provider, and a security manager is
     *         installed, the {@code SecurityManager.checkDelete(String)} method
     *         is invoked to check delete access to the file.
     * @throws Exception
     *         if thrown by the closure.
     */
    static <T> T withFile(Path self, @ClosureParams(value = FirstParam.class) Closure<T> block) throws IOException {
        return withResource(self, deletePath, block)
    }

    /**
     * Passes this File to the closure, ensuring that the file is deleted after the closure returns,
     * regardless of errors.
     *
     * As with the try-with-resources statement, if multiple exceptions are thrown, the exception from the closure
     * will be returned and the exception from deleting will be added as a suppressed exception.
     *
     * Usage example:
     *
     * {@Code
     * File.createTempFile('tst', null).withFile { tmpFile ->
     *     // Do something with your temp file.
     *     // If any exception is thrown, the file will be deleted.
     * }
     * // The file has been deleted
     * }
     *
     * @param self                      the {@code File}.
     * @param block                     the closure taking the {@code File} as parameter.
     * @return                          the value returned by the closure.
     * @throws IllegalArgumentException if this abstract path is not a valid path.
     * @throws IOException              if an I/O error occurs while deleting the file.
     * @throws SecurityException        if a security manager is installed,
     *                                  the {@code SecurityManager.checkDelete(String)} method
     *                                  is invoked to check delete access to the file.
     * @throws Exception                if thrown by the closure.
     */
    static <T> T withFile(File self, @ClosureParams(value = FirstParam.class) Closure<T> block) throws IOException {
        return withResource(self, deleteFile, block)
    }

    /**
     * Passes this Path to the closure, ensuring that the file is deleted
     * whenever the closure throws an exception.
     *
     * This method is typically used to initialise a file for further use.

     * If the deletion throws an exception, the exception from the closure
     * will be returned and the exception from deleting will be added as a suppressed exception.
     *
     * Usage example:
     *
     * {@Code
     * def file = Files.createTempFile('tst', null)
     * file.initFile { tmpFile ->
     *     // Initialize the file.
     *     // If any exception is thrown, the file will be deleted.
     * }
     * // The file has been successfully initialized and is available for usage.
     * }
     *
     * @param self       the {@code Path}.
     * @param block      the closure taking the {@code Path} as parameter.
     * @return           the value returned by the closure.
     * @throws Exception if thrown by the closure.
     */
    static <T> T initFile(Path self, @ClosureParams(value = FirstParam.class) Closure<T> block) {
        return initResource(self, deletePath, block)
    }

    /**
     * Passes this File to the closure, ensuring that the file is deleted
     * whenever the closure throws an exception.
     *
     * This method is typically used to initialise a file for further use.

     * If the deletion throws an exception, the exception from the closure
     * will be returned and the exception from deleting will be added as a suppressed exception.
     *
     * Usage example:
     *
     * {@Code
     * def file = File.createTempFile('tst', null)
     * file.initFile { tmpFile ->
     *     // Initialize the file.
     *     // If any exception is thrown, the file will be deleted.
     * }
     * // The file has been successfully initialized and is available for usage.
     * }
     *
     * @param self       the {@code File}.
     * @param block      the closure taking the {@code File} as parameter.
     * @return           the value returned by the closure.
     * @throws Exception if thrown by the closure.
     */
    static <T> T initFile(File self, @ClosureParams(value = FirstParam.class) Closure<T> block) {
        return initResource(self, deleteFile, block)
    }

    /**
     * Passes this Path to the closure, ensuring that the directory and all its contents
     * are deleted after the closure returns, regardless of errors.
     *
     * The Path must represent a directory.
     *
     * As with the try-with-resources statement, if multiple exceptions are thrown, the exception from the closure
     * will be returned and the exception from deleting will be added as a suppressed exception.
     *
     * Usage example:
     *
     * {@Code
     * Files.createTempDirectory('tst').withDir { tmpDir ->
     *     // Do something in your temp directory.
     *     // If any exception is thrown, the directory and all of its contents will be deleted.
     * }
     * // The directory and all of its contents has been deleted
     * }
     *
     * @param self                           the {@code Path}.
     * @param cleanContentsOnly              if true, the contents will be deleted, but not the directory itself.
     *                                       Default: false.
     * @param block                          the closure taking the {@code Path} as parameter.
     * @return                               the value returned by the closure.
     * @throws IllegalArgumentException      if the file is not a directory.
     * @throws IOException                   if an I/O error occurs while deleting the directory and its contents.
     * @throws SecurityException             In the case of the default provider, and a security manager is
     *                                       installed, the {@code SecurityManager.checkDelete(String)} method
     *                                       is invoked to check delete access to the directory and its contents.
     * @throws Exception                     if thrown by the closure.
     */
    static <T> T withDir(Path self,
                         boolean cleanContentsOnly = false,
                         @ClosureParams(value = FirstParam.class) Closure<T> block) throws IOException {
        return withResource(self, cleanContentsOnly ? deleteDirContentsPath : deleteDirPath, block)
    }

    /**
     * Passes this File to the closure, ensuring that the directory and all its contents
     * are deleted after the closure returns, regardless of errors.
     *
     * The File must represent a directory.
     *
     * As with the try-with-resources statement, if multiple exceptions are thrown, the exception from the closure
     * will be returned and the exception from deleting will be added as a suppressed exception.
     *
     * Usage example:
     *
     * {@Code
     * File.createTempDir().withDir { tmpDir ->
     *     // Do something in your temp directory.
     *     // If any exception is thrown, the directory and all of its contents will be deleted.
     * }
     * // The directory and all of its contents has been deleted
     * }
     *
     * @param self                      the {@code File}.
     * @param cleanContentsOnly         if true, the contents will be deleted, but not the directory itself.
     *                                  Default: false.
     * @param block                     the closure taking the {@code File} as parameter.
     * @return                          the value returned by the closure.
     * @throws IllegalArgumentException if this abstract path is not a valid path
     *                                  or is not a directory.
     * @throws IOException              if an I/O error occurs while deleting the directory and its contents.
     * @throws SecurityException        if a security manager is installed,
     *                                  the {@code SecurityManager.checkDelete(String)} method
     *                                  is invoked to check delete access to the directory and its contents.
     * @throws Exception                if thrown by the closure.
     */
    static <T> T withDir(File self,
                         boolean cleanContentsOnly = false,
                         @ClosureParams(value = FirstParam.class) Closure<T> block) throws IOException {
        return withResource(self, cleanContentsOnly ? deleteDirContents : deleteDir, block)
    }

    /**
     * Passes this Path to the closure, ensuring that the directory and all its contents are deleted
     * whenever the closure throws an exception.
     *
     * The Path must represent a directory.
     *
     * This method is typically used to initialise a directory for further use.
     *
     * If the deletion throws an exception, the exception from the closure
     * will be returned and the exception from deleting will be added as a suppressed exception.
     *
     * Usage example:
     *
     * {@Code
     * def directory = Files.createTempDirectory('tst')
     * directory.initDir { tmpDir ->
     *     // Initialize the directory and its contents.
     *     // If any exception is thrown, the directory and all of its contents will be deleted.
     * }
     * // The directory has been successfully initialized and is available for usage.
     * }
     *
     * @param self              the {@code Path}.
     * @param cleanContentsOnly if true, in case of exception, the contents will be deleted,
     *                          but not the directory itself. Default: false.
     * @param block             the closure taking the {@code Path} as parameter.
     * @return                  the value returned by the closure.
     * @throws Exception        if thrown by the closure.
     */
    static <T> T initDir(Path self,
                         boolean cleanContentsOnly = false,
                         @ClosureParams(value = FirstParam.class) Closure<T> block) {
        return initResource(self, cleanContentsOnly ? deleteDirContentsPath : deleteDirPath, block)
    }

    /**
     * Passes this File to the closure, ensuring that the directory and all its contents are deleted
     * whenever the closure throws an exception.
     *
     * The File must represent a directory.
     *
     * This method is typically used to initialise a directory for further use.
     *
     * If the deletion throws an exception, the exception from the closure
     * will be returned and the exception from deleting will be added as a suppressed exception.
     *
     * Usage example:
     *
     * {@Code
     * def directory = File.createTempDir()
     * directory.initDir { tmpDir ->
     *     // Initialize the directory and its contents.
     *     // If any exception is thrown, the directory and all of its contents will be deleted.
     * }
     * // The directory has been successfully initialized and is available for usage.
     * }
     *
     * @param self              the {@code File}.
     * @param cleanContentsOnly if true, in case of exception, the contents will be deleted,
     *                          but not the directory itself. Default: false.
     * @param block             the closure taking the {@code File} as parameter.
     * @return                  the value returned by the closure.
     * @throws Exception        if thrown by the closure.
     */
    static <T> T initDir(File self,
                         boolean cleanContentsOnly = false,
                         @ClosureParams(value = FirstParam.class) Closure<T> block) {
        return initResource(self, cleanContentsOnly ? deleteDirContents : deleteDir, block)
    }

    /**
     * Allows this resource to be used within the {@code body} closure, ensuring that {@code cleanup}
     * has been invoked once the closure has been executed and before this method returns.
     *
     * If no cleanup closure is provided, by default it will invoke the close method of the resource.
     *
     * As with the try-with-resources statement, if multiple exceptions are thrown
     * the exception from the closure will be returned and the exception from the cleanup
     * will be added as a suppressed exception.
     *
     * Usage example:
     *
     * {@Code
     * Files.createTempDirectory('tst').withResource({ it.deleteDir() }) { tmpDir ->
     *     // Do something in your temp directory.
     *     // If any exception is thrown, the directory and all of its contents will be deleted.
     * }
     * // The directory and all of its contents has been deleted
     * }
     *
     * @param identifier the resource for which to run the provided {@code block} closure.
     * @param cleanup    the code to perform the desired cleanup on the resource,
     *                   or {@code { it.close() }}, if not provided.
     * @param block      the code that will make use of the resource.
     * @return           the value returned by {@code block}.
     * @throws Exception if thrown by either {@code block} or {@code cleanup}.
     */
    static <T, R> T withResource(R identifier,
                                 @ClosureParams(value = FirstParam.class) Closure cleanup = { it.close() },
                                 @ClosureParams(value = FirstParam.class) Closure<T> block) {
        Throwable primaryExc = null
        try {
            return block(identifier)
        } catch (Throwable t) {
            primaryExc = t
            throw t
        } finally {
            if (identifier != null) {
                if (primaryExc != null) {
                    try {
                        cleanup(identifier)
                    } catch (Throwable suppressedExc) {
                        primaryExc.addSuppressed(suppressedExc)
                    }
                } else {
                    cleanup(identifier)
                }
            }
        }
    }

    /**
     * Allows this resource to be used within the {@code body} closure, ensuring that {@code cleanup}
     * has been invoked whenever the closure has thrown an exception.
     *
     * If no cleanup closure is provided, by default it will invoke the close method of the resource.
     *
     * If the cleanup throws an exception,
     * the exception from the closure will be returned and the exception from the cleanup
     * will be added as a suppressed exception.
     *
     * This method is typically used to initialize some resource for later usage.
     *
     * Usage example:
     *
     * {@Code
     * def directory = Files.createTempDirectory('tst')
     * directory.withResource({ it.deleteDir() }) { tmpDir ->
     *     // Initialize the directory and its contents.
     *     // If any exception is thrown, the directory and all of its contents will be deleted.
     * }
     * // The directory has been successfully initialized and is available for usage.
     * }
     *
     * @param identifier the resource for which to run the provided {@code block} closure.
     * @param cleanup    the code to perform the desired cleanup on the resource,
     *                   or {@code { it.close() }}, if not provided.
     * @param block      the code that will make use of the resource.
     * @return           the value returned by {@code block}.
     * @throws Exception if thrown by {@code block}.
     */
    static <T, R> T initResource(R identifier,
                                 @ClosureParams(value = FirstParam.class) Closure cleanup = { it.close() },
                                 @ClosureParams(value = FirstParam.class) Closure<T> block) {
        try {
            return block(identifier)
        } catch (Throwable t) {
            if (identifier != null) {
                try {
                    cleanup(identifier)
                } catch (Throwable suppressedExc) {
                    t.addSuppressed(suppressedExc)
                }
            }
            throw t
        }
    }

}
