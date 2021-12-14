package util

import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FirstParam
import groovy.transform.stc.SimpleType

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileAttribute

/**
 * This is a set of tools for file creation, mainly temporary files or directories
 * that must be deleted in case of error or when their processing completes.
 */
class FileTools {

    private FileTools() {}

    /**
     * Creates a new temporary file by invoking the method {@code Files.createTempFile} with the given parameters
     * and passes the newly created {@code Path} instance to the given closure.
     * The attrs are converted to an array in order to invoke {@code createTempFile}
     * The file will be created in the given directory.
     *
     * It is warranted that the file will be deleted upon exit from the closure,
     * whether it succeeds or throws an exception.
     *
     * Sample usage:
     *
     * {@Code
     * Path parent = ...
     * FileTools.withTempFile(parent, 'tst') { tmpFile ->
     *     // Do something with your temp file.
     *     // If any exception is thrown, the file will be deleted
     * }
     * // The file has been deleted
     * }
     *
     * @param dir    the directory to create the temporary file in.
     * @param prefix the prefix for the new temporary file name.
     * @param suffix the suffix for the new temporary file name. If null or not present, '.tmp' will be used.
     * @param attrs  an optional list of file attributes to set atomically when creating the file.
     * @param block  a closure to execute with the newly created temporary file.
     * @return       the value returned by the closure.
     * @throws  IllegalArgumentException
     *          if the prefix or suffix parameters cannot be used to generate
     *          a candidate file name.
     * @throws  UnsupportedOperationException
     *          if the {@code attrs} list contains an attribute that cannot be set atomically
     *          when creating the file.
     * @throws IOException
     *         if an I/O error occurs or {@code dir} does not exist.
     * @throws SecurityException
     *         In the case of the default provider, and a security manager is
     *         installed, the {@code SecurityManager.checkWrite(String)}
     *         method is invoked to check write access to the file
     *         and the {@code SecurityManager.checkDelete(String)} method
     *         is invoked to check delete access to the file.
     * @throws Exception
     *         if thrown by the closure.
     */
    static <T> T withTempFile(Path dir,
                              String prefix,
                              String suffix = null,
                              List<FileAttribute<?>> attrs = [],
                              @ClosureParams(value = FirstParam.class) Closure<T> block) throws IOException {
        return Files.createTempFile(dir, prefix, suffix, attrs as FileAttribute<?>[]).withFile { tmpFile ->
            return block(tmpFile)
        }
    }

    /**
     * Creates a new temporary file by invoking the method {@code Files.createTempFile} with the given parameters
     * and passes the newly created {@code Path} instance to the given closure.
     * The attrs are converted to an array in order to invoke {@code createTempFile}
     * The file will be created in the default temporary-file directory.
     *
     * It is warranted that the file will be deleted upon exit from the closure,
     * whether it succeeds or throws an exception.
     *
     * Sample usage:
     *
     * {@Code
     * FileTools.withTempFile('tst') { tmpFile ->
     *     // Do something with your temp file.
     *     // If any exception is thrown, the file will be deleted
     * }
     * // The file has been deleted
     * }
     *
     * @param prefix the prefix for the new temporary file name.
     * @param suffix the suffix for the new temporary file name. If null or not present, '.tmp' will be used.
     * @param attrs  an optional list of file attributes to set atomically when creating the file.
     * @param block  a closure to execute with the newly created temporary file.
     * @return       the value returned by the closure.
     * @throws  IllegalArgumentException
     *          if the prefix or suffix parameters cannot be used to generate
     *          a candidate file name.
     * @throws  UnsupportedOperationException
     *          if the {@code attrs} list contains an attribute that cannot be set atomically
     *          when creating the file.
     * @throws IOException
     *         if an I/O error occurs or the temporary-file directory does not exist.
     * @throws SecurityException
     *         In the case of the default provider, and a security manager is
     *         installed, the {@code SecurityManager.checkWrite(String)}
     *         method is invoked to check write access to the file
     *         and the {@code SecurityManager.checkDelete(String)} method
     *         is invoked to check delete access to the file.
     * @throws Exception
     *         if thrown by the closure.
     */
    static <T> T withTempFile(String prefix,
                              String suffix = null,
                              List<FileAttribute<?>> attrs = [],
                              @ClosureParams(value = SimpleType.class, options = ['java.nio.file.Path'])
                                          Closure<T> block) throws IOException {
        return Files.createTempFile(prefix, suffix, attrs as FileAttribute<?>[]).withFile { tmpFile ->
            return block(tmpFile)
        }
    }

    /**
     * Creates a new temporary file by invoking the method {@code Files.createTempFile} with the given parameters
     * and passes the newly created {@code Path} instance to the given closure for initialisation.
     * The attrs are converted to an array in order to invoke {@code createTempFile}
     * The file will be created in the given directory.
     *
     * It is warranted that the file will be deleted in case the closure throws an exception.
     *
     * Sample usage:
     *
     * {@Code
     * Path parent = ...
     * def file = FileTools.newTempFile(parent, 'tst') { tmpFile ->
     *     // Initialize your temp file, possibly adding contents to it.
     *     // If any exception is thrown, the file will be deleted
     * }
     * // The file has been successfully initialized and is available for usage.
     * }
     *
     * @param dir    the directory to create the temporary file in.
     * @param prefix the prefix for the new temporary file name.
     * @param suffix the suffix for the new temporary file name. If null or not present, '.tmp' will be used.
     * @param attrs  an optional list of file attributes to set atomically when creating the file.
     * @param init   a closure to initialise the temporary file.
     * @return       the newly created {@code Path} instance.
     * @throws  IllegalArgumentException
     *          if the prefix or suffix parameters cannot be used to generate
     *          a candidate file name.
     * @throws  UnsupportedOperationException
     *          if the {@code attrs} list contains an attribute that cannot be set atomically
     *          when creating the file.
     * @throws IOException
     *         if an I/O error occurs or {@code dir} does not exist.
     * @throws SecurityException
     *         In the case of the default provider, and a security manager is
     *         installed, the {@code SecurityManager.checkWrite(String)}
     *         method is invoked to check write access to the file.
     * @throws Exception
     *         if thrown by the closure.
     */
    static Path newTempFile(Path dir,
                            String prefix,
                            String suffix = null,
                            List<FileAttribute<?>> attrs = [],
                            @ClosureParams(value = FirstParam.class) Closure<?> init) throws IOException {
        return Files.createTempFile(dir, prefix, suffix, attrs as FileAttribute<?>[]).initFile { tmpFile ->
            init(tmpFile)
            return tmpFile
        }
    }

    /**
     * Creates a new temporary file by invoking the method {@code Files.createTempFile} with the given parameters
     * and passes the newly created {@code Path} instance to the given closure for initialisation.
     * The attrs are converted to an array in order to invoke {@code createTempFile}
     * The file will be created in the default temporary-file directory.
     *
     * It is warranted that the file will be deleted in case the closure throws an exception.
     *
     * Sample usage:
     *
     * {@Code
     * def file = FileTools.newTempFile('tst') { tmpFile ->
     *     // Initialize your temp file, possibly adding contents to it.
     *     // If any exception is thrown, the file will be deleted
     * }
     * // The file has been successfully initialized and is available for usage.
     * }
     *
     * @param prefix the prefix for the new temporary file name.
     * @param suffix the suffix for the new temporary file name. If null or not present, '.tmp' will be used.
     * @param attrs  an optional list of file attributes to set atomically when creating the file.
     * @param init   a closure to initialise the temporary file.
     * @return       the newly created {@code Path} instance.
     * @throws  IllegalArgumentException
     *          if the prefix or suffix parameters cannot be used to generate
     *          a candidate file name.
     * @throws  UnsupportedOperationException
     *          if the {@code attrs} list contains an attribute that cannot be set atomically
     *          when creating the file.
     * @throws IOException
     *         if an I/O error occurs or the temporary-file directory does not exist.
     * @throws SecurityException
     *         In the case of the default provider, and a security manager is
     *         installed, the {@code SecurityManager.checkWrite(String)}
     *         method is invoked to check write access to the file.
     * @throws Exception
     *         if thrown by the closure.
     */
    static Path newTempFile(String prefix,
                            String suffix = null,
                            List<FileAttribute<?>> attrs = [],
                            @ClosureParams(value = SimpleType.class, options = ['java.nio.file.Path'])
                                         Closure<?> init) throws IOException {
        return Files.createTempFile(prefix, suffix, attrs as FileAttribute<?>[]).initFile { tmpFile ->
            init(tmpFile)
            return tmpFile
        }
    }

    /**
     * Creates a new temporary directory by invoking the method {@code Files.createTempDirectory}
     * with the given parameters and passes the newly created {@code Path} instance to the given closure.
     * The attrs are converted to an array in order to invoke {@code createTempDirectory}
     * The new directory will be created in the given parent directory.
     *
     * It is warranted that the directory and all its contents will be deleted upon exit from the closure,
     * whether it succeeds or throws an exception.
     *
     * Sample usage:
     *
     * {@Code
     * Path parent = ...
     * FileTools.withTempDir(parent, 'tst') { tmpDir ->
     *     // Do something with your temp directory.
     *     // If any exception is thrown, the directory and its contents will be deleted.
     * }
     * // The directory and its contents have been deleted.
     * }
     *
     * @param dir    the parent directory to create the temporary file in.
     * @param prefix the prefix for the new temporary directory name.
     * @param attrs  an optional list of file attributes to set atomically when creating the directory.
     * @param block  a closure to execute with the newly created temporary directory.
     * @return       the value returned by the closure.
     * @throws  IllegalArgumentException
     *          if the prefix cannot be used to generate
     *          a candidate directory name.
     * @throws  UnsupportedOperationException
     *          if the {@code attrs} list contains an attribute that cannot be set atomically
     *          when creating the directory.
     * @throws IOException
     *         if an I/O error occurs or {@code dir} does not exist.
     * @throws SecurityException
     *         In the case of the default provider, and a security manager is
     *         installed, the {@code SecurityManager.checkWrite(String)}
     *         method is invoked to check write access when creating the directory
     *         and the {@code SecurityManager.checkDelete(String)} method
     *         is invoked to check delete access to the directory and its contents.
     * @throws Exception
     *         if thrown by the closure.
     */
    static <T> T withTempDir(Path dir,
                             String prefix,
                             List<FileAttribute<?>> attrs = [],
                             @ClosureParams(value = FirstParam.class) Closure<T> block) throws IOException {
        return Files.createTempDirectory(dir, prefix, attrs as FileAttribute<?>[]).withDir { tmpDir ->
            return block(tmpDir)
        }
    }

    /**
     * Creates a new temporary directory by invoking the method {@code Files.createTempDirectory}
     * with the given parameters and passes the newly created {@code Path} instance to the given closure.
     * The attrs are converted to an array in order to invoke {@code createTempDirectory}
     * The new directory will be created in the default temporary-file directory.
     *
     * It is warranted that the directory and all its contents will be deleted upon exit from the closure,
     * whether it succeeds or throws an exception.
     *
     * Sample usage:
     *
     * {@Code
     * FileTools.withTempDir('tst') { tmpDir ->
     *     // Do something with your temp directory.
     *     // If any exception is thrown, the directory and its contents will be deleted.
     * }
     * // The directory and its contents have been deleted.
     * }
     *
     * @param prefix the prefix for the new temporary directory name.
     * @param attrs  an optional list of file attributes to set atomically when creating the directory.
     * @param block  a closure to execute with the newly created temporary directory.
     * @return       the value returned by the closure.
     * @throws  IllegalArgumentException
     *          if the prefix cannot be used to generate
     *          a candidate directory name.
     * @throws  UnsupportedOperationException
     *          if the {@code attrs} list contains an attribute that cannot be set atomically
     *          when creating the directory.
     * @throws IOException
     *         if an I/O error occurs or the temporary-file directory does not exist.
     * @throws SecurityException
     *         In the case of the default provider, and a security manager is
     *         installed, the {@code SecurityManager.checkWrite(String)}
     *         method is invoked to check write access when creating the directory
     *         and the {@code SecurityManager.checkDelete(String)} method
     *         is invoked to check delete access to the directory and its contents.
     * @throws Exception
     *         if thrown by the closure.
     */
    static <T> T withTempDir(String prefix,
                             List<FileAttribute<?>> attrs = [],
                             @ClosureParams(value = SimpleType.class, options = ['java.nio.file.Path'])
                                         Closure<T> block) throws IOException {
        return Files.createTempDirectory(prefix, attrs as FileAttribute<?>[]).withDir { tmpDir ->
            return block(tmpDir)
        }
    }

    /**
     * Creates a new temporary directory by invoking the method {@code Files.createTempDirectory}
     * with the given parameters and passes the newly created {@code Path} instance to the given closure.
     * The attrs are converted to an array in order to invoke {@code createTempDirectory}
     * The new directory will be created in the given parent directory.
     *
     * It is warranted that the directory and all its contents will be deleted if the closure throws an exception.
     *
     * Sample usage:
     *
     * {@Code
     * Path parent = ...
     * def directory = FileTools.newTempDir(parent, 'tst') { tmpDir ->
     *     // Initialise your temp directory, possibly adding contents to it.
     *     // If any exception is thrown, the directory and its contents will be deleted.
     * }
     * // The directory has been successfully initialized and is available for usage.
     * }
     *
     * @param dir    the parent directory to create the temporary file in.
     * @param prefix the prefix for the new temporary directory name.
     * @param attrs  an optional list of file attributes to set atomically when creating the directory.
     * @param init   a closure to execute with the newly created temporary directory.
     * @return       the value returned by the closure.
     * @throws  IllegalArgumentException
     *          if the prefix cannot be used to generate
     *          a candidate directory name.
     * @throws  UnsupportedOperationException
     *          if the {@code attrs} list contains an attribute that cannot be set atomically
     *          when creating the directory.
     * @throws IOException
     *         if an I/O error occurs or {@code dir} does not exist.
     * @throws SecurityException
     *         In the case of the default provider, and a security manager is
     *         installed, the {@code SecurityManager.checkWrite(String)}
     *         method is invoked to check write access when creating the directory.
     * @throws Exception
     *         if thrown by the closure.
     */
    static Path newTempDir(Path dir,
                           String prefix,
                           List<FileAttribute<?>> attrs = [],
                           @ClosureParams(value = FirstParam.class) Closure<?> init) throws IOException {
        return Files.createTempDirectory(dir, prefix, attrs as FileAttribute<?>[]).initDir { tmpDir ->
            init(tmpDir)
            return tmpDir
        }
    }

    /**
     * Creates a new temporary directory by invoking the method {@code Files.createTempDirectory}
     * with the given parameters and passes the newly created {@code Path} instance to the given closure.
     * The attrs are converted to an array in order to invoke {@code createTempDirectory}
     * The new directory will be created in the default temporary-file directory.
     *
     * It is warranted that the directory and all its contents will be deleted if the closure throws an exception.
     *
     * Sample usage:
     *
     * {@Code
     * def directory = FileTools.newTempDir('tst') { tmpDir ->
     *     // Initialise your temp directory, possibly adding contents to it.
     *     // If any exception is thrown, the directory and its contents will be deleted.
     * }
     * // The directory has been successfully initialized and is available for usage.
     * }
     *
     * @param dir    the parent directory to create the temporary file in.
     * @param prefix the prefix for the new temporary directory name.
     * @param attrs  an optional list of file attributes to set atomically when creating the directory.
     * @param init   a closure to execute with the newly created temporary directory.
     * @return       the value returned by the closure.
     * @throws  IllegalArgumentException
     *          if the prefix cannot be used to generate
     *          a candidate directory name.
     * @throws  UnsupportedOperationException
     *          if the {@code attrs} list contains an attribute that cannot be set atomically
     *          when creating the directory.
     * @throws IOException
     *         if an I/O error occurs or the temporary-file directory does not exist.
     * @throws SecurityException
     *         In the case of the default provider, and a security manager is
     *         installed, the {@code SecurityManager.checkWrite(String)}
     *         method is invoked to check write access when creating the directory.
     * @throws Exception
     *         if thrown by the closure.
     */
    static Path newTempDir(String prefix,
                           List<FileAttribute<?>> attrs = [],
                           @ClosureParams(value = SimpleType.class, options = ['java.nio.file.Path'])
                                        Closure<?> init) throws IOException {
        return Files.createTempDirectory(prefix, attrs as FileAttribute<?>[]).initDir { tmpDir ->
            init(tmpDir)
            return tmpDir
        }
    }

}
