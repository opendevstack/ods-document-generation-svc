package util

import spock.lang.Specification

import java.nio.file.DirectoryNotEmptyException
import java.nio.file.Files

class TrySpec extends Specification {

    def "test withFile_File"() {
        given:
        def file
        def ret
        def e

        when: 'The closure completes successfully'
        file = File.createTempFile('tst', null)
        ret = file.withFile { return 1 }

        then: 'The return value is that of the closure and the file has been deleted'
        ret == 1
        !file.exists()

        when: 'The closure throws an exception'
        file = File.createTempFile('tst', null)
        file.withFile { throw new IndexOutOfBoundsException() }

        then: 'The exception is propagated and the file has been deleted'
        thrown(IndexOutOfBoundsException)
        !file.exists()

        when: 'The closure completes successfully, but the file does not exist'
        file = File.createTempFile('tst', null)
        file.delete()
        ret = file.withFile { return 2 }

        then: 'The return value is that of the closure and no exception is thrown when trying to delete the file'
        notThrown(IOException)
        ret == 2

        when: 'The closure throws an exception and the file does not exist'
        file.withFile { throw new IndexOutOfBoundsException() }

        then: 'The exception is propagated and no exception is suppressed when trying to delete the file'
        e = thrown(IndexOutOfBoundsException)
        e.suppressed.length == 0

        when: 'The closure completes successfully, but the file path is invalid'
        file = new File('Q:\\\\\0')
        file.withFile { return 3 }

        then: 'The IllegalArgumentException thrown when trying to delete the file is propagated'
        thrown(IllegalArgumentException)

        when: 'The closure throws an exception and the file path is invalid'
        file = new File('Q:\\\\\0')
        file.withFile { throw new IndexOutOfBoundsException() }

        then: 'The exception is propagated and IllegalArgumentException is suppressed when trying to delete the file'
        e = thrown(IndexOutOfBoundsException)
        e.suppressed[0] instanceof IllegalArgumentException

        when: 'The closure completes successfully, but the file cannot be deleted'
        file = File.createTempDir()
        File.createTempFile('tst', null, file)
        file.withFile { return 3 }

        then: 'The IOException thrown when trying to delete the file is propagated'
        thrown(IOException)

        cleanup:
        file.deleteDir()
    }

    def "test initFile_File"() {
        given:
        def file
        def ret
        def e

        when: 'The closure completes successfully'
        file = File.createTempFile('tst', null)
        ret = file.initFile { return 1 }

        then: 'The return value is that of the closure and the file has not been deleted'
        ret == 1
        file.exists()

        when: 'The closure throws an exception'
        file.initFile { throw new IndexOutOfBoundsException() }

        then: 'The exception is propagated and the file has been deleted'
        thrown(IndexOutOfBoundsException)
        !file.exists()

        when: 'The closure throws an exception and the file does not exist'
        file.initFile { throw new IndexOutOfBoundsException() }

        then: 'The exception is propagated and no exception is suppressed when trying to delete the file'
        e = thrown(IndexOutOfBoundsException)
        e.suppressed.length == 0

        when: 'The closure throws an exception and the file path is invalid'
        file = new File('Q:\\\\\0')
        file.initFile { throw new IndexOutOfBoundsException() }

        then: 'The exception is propagated and IllegalArgumentException is suppressed when trying to delete the file'
        e = thrown(IndexOutOfBoundsException)
        e.suppressed[0] instanceof IllegalArgumentException

        when: 'The closure throws an exception, but the file cannot be deleted'
        file = File.createTempDir()
        File.createTempFile('tst', null, file)
        file.initFile { throw new IndexOutOfBoundsException() }

        then: 'The exception is propagated and the IOException thrown when trying to delete the file is suppressed'
        e = thrown(IndexOutOfBoundsException)
        e.suppressed[0] instanceof IOException

        cleanup:
        file.deleteDir()
    }

    def "test withFile_Path"() {
        given:
        def path
        def ret
        def e

        when: 'The closure completes successfully'
        path = Files.createTempFile('tst', null)
        ret = path.withFile { return 1 }

        then: 'The return value is that of the closure and the file has been deleted'
        ret == 1
        Files.notExists(path)

        when: 'The closure throws an exception'
        path = Files.createTempFile('tst', null)
        path.withFile { throw new IndexOutOfBoundsException() }

        then: 'The exception is propagated and the file has been deleted'
        thrown(IndexOutOfBoundsException)
        Files.notExists(path)

        when: 'The closure completes successfully, but the file does not exist'
        ret = path.withFile { return 2 }

        then: 'The return value is that of the closure and no exception is thrown when trying to delete the file'
        notThrown(IOException)
        ret == 2

        when: 'The closure throws an exception and the file does not exist'
        path.withFile { throw new IndexOutOfBoundsException() }

        then: 'The exception is propagated and no exception is suppressed when trying to delete the file'
        e = thrown(IndexOutOfBoundsException)
        e.suppressed.length == 0

        when: 'The closure completes successfully, but the file is a non-empty directory'
        path = Files.createTempDirectory('tst')
        Files.createTempFile(path, 'tst', null)
        path.withFile { return 3 }

        then: 'The DirectoryNotEmptyException thrown when trying to delete the file is propagated'
        thrown(DirectoryNotEmptyException)

        when: 'The closure throws an exception and the file is a non-empty directory'
        path.withFile { throw new IndexOutOfBoundsException() }

        then: 'The exception is propagated and IllegalArgumentException is suppressed when trying to delete the file'
        e = thrown(IndexOutOfBoundsException)
        e.suppressed[0] instanceof DirectoryNotEmptyException

        cleanup:
        path.deleteDir()
    }

    def "test initFile_Path"() {
        given:
        def path
        def ret
        def e

        when: 'The closure completes successfully'
        path = Files.createTempFile('tst', null)
        ret = path.initFile { return 1 }

        then: 'The return value is that of the closure and the file has not been deleted'
        ret == 1
        Files.exists(path)

        when: 'The closure throws an exception'
        path.initFile { throw new IndexOutOfBoundsException() }

        then: 'The exception is propagated and the file has been deleted'
        thrown(IndexOutOfBoundsException)
        Files.notExists(path)

        when: 'The closure throws an exception and the file does not exist'
        path.initFile { throw new IndexOutOfBoundsException() }

        then: 'The exception is propagated and no exception is suppressed when trying to delete the file'
        e = thrown(IndexOutOfBoundsException)
        e.suppressed.length == 0

        when: 'The closure throws an exception and the file is a non-empty directory'
        path = Files.createTempDirectory('tst')
        Files.createTempFile(path, 'tst', null)
        path.initFile { throw new IndexOutOfBoundsException() }

        then: 'The exception is propagated and DirectoryNotEmptyException is suppressed when trying to delete the file'
        e = thrown(IndexOutOfBoundsException)
        e.suppressed[0] instanceof DirectoryNotEmptyException

        cleanup:
        path.deleteDir()
    }

    def "test withDir_File"() {
        given:
        def dir
        def contents
        def ret
        def e

        when: 'The closure completes successfully. Delete the contents only.'
        dir = File.createTempDir()
        contents = File.createTempFile('tst', null, dir)
        ret = dir.withDir(true) { return 1 }

        then: 'The return value is that of the closure and the directory only has been deleted'
        ret == 1
        dir.exists()
        !contents.exists()

        when: 'The closure completes successfully'
        File.createTempFile('tst', null, dir)
        ret = dir.withDir { return 1 }

        then: 'The return value is that of the closure and the directory has been deleted'
        ret == 1
        !dir.exists()

        when: 'The closure throws an exception. Delete the contents only.'
        dir = File.createTempDir()
        contents = File.createTempFile('tst', null, dir)
        dir.withDir(true) { throw new IndexOutOfBoundsException() }

        then: 'The exception is propagated and the directory contents only has been deleted'
        thrown(IndexOutOfBoundsException)
        dir.exists()
        !contents.exists()

        when: 'The closure throws an exception'
        File.createTempFile('tst', null, dir)
        dir.withDir { throw new IndexOutOfBoundsException() }

        then: 'The exception is propagated and the directory has been deleted'
        thrown(IndexOutOfBoundsException)
        !dir.exists()

        when: 'The closure completes successfully, but the directory does not exist'
        ret = dir.withDir { return 2 }

        then: 'The return value is that of the closure and no exception is thrown when trying to delete the directory'
        notThrown(IOException)
        ret == 2

        when: 'The closure throws an exception and the file does not exist'
        dir.withDir { throw new IndexOutOfBoundsException() }

        then: 'The exception is propagated and no exception is suppressed when trying to delete the directory'
        e = thrown(IndexOutOfBoundsException)
        e.suppressed.length == 0

        when: 'The closure completes successfully, but the directory path is invalid'
        dir = new File('Q:\\\\\0')
        dir.withDir { return 3 }

        then: 'The IllegalArgumentException thrown when trying to delete the directory is propagated'
        thrown(IllegalArgumentException)

        when: 'The closure throws an exception and the directory path is invalid'
        dir = new File('Q:\\\\\0')
        dir.withDir { throw new IndexOutOfBoundsException() }

        then: 'The exception is propagated and IllegalArgumentException is suppressed when trying to delete the directory'
        e = thrown(IndexOutOfBoundsException)
        e.suppressed[0] instanceof IllegalArgumentException

        when: 'The closure completes successfully, but the file is not a directory'
        dir = File.createTempFile('tst', null)
        dir.withDir { return 3 }

        then: 'The IllegalArgumentException thrown when trying to delete the directory is propagated'
        thrown(IllegalArgumentException)

        cleanup:
        dir?.delete()
        contents?.delete()
    }

    def "test initDir_File"() {
        given:
        def dir
        def contents
        def ret
        def e

        when: 'The closure completes successfully'
        dir = File.createTempDir()
        contents = File.createTempFile('tst', null, dir)
        ret = dir.initDir { return 1 }

        then: 'The return value is that of the closure and neither the directory nor its contents have been deleted'
        ret == 1
        dir.exists()
        contents.exists()

        when: 'The closure throws an exception. Delete the contents only'
        dir.initDir(true) { throw new IndexOutOfBoundsException() }

        then: 'The exception is propagated and the directory contents only has been deleted'
        thrown(IndexOutOfBoundsException)
        dir.exists()
        !contents.exists()

        when: 'The closure throws an exception'
        File.createTempFile('tst', null, dir)
        dir.initDir { throw new IndexOutOfBoundsException() }

        then: 'The exception is propagated and the directory has been deleted'
        thrown(IndexOutOfBoundsException)
        !dir.exists()

        when: 'The closure throws an exception and the directory does not exist'
        dir.initDir { throw new IndexOutOfBoundsException() }

        then: 'The exception is propagated and no exception is suppressed when trying to delete the directory'
        e = thrown(IndexOutOfBoundsException)
        e.suppressed.length == 0

        when: 'The closure throws an exception and the directory path is invalid'
        dir = new File('Q:\\\\\0')
        dir.initDir { throw new IndexOutOfBoundsException() }

        then: 'The exception is propagated and IllegalArgumentException is suppressed when trying to delete the directory'
        e = thrown(IndexOutOfBoundsException)
        e.suppressed[0] instanceof IllegalArgumentException

        when: 'The closure throws an exception, but the file is not a directory'
        dir = File.createTempFile('tst', null)
        dir.initDir {throw new IndexOutOfBoundsException() }

        then: 'The IllegalArgumentException thrown when trying to delete the directory is propagated'
        e = thrown(IndexOutOfBoundsException)
        e.suppressed[0] instanceof IllegalArgumentException

        cleanup:
        dir?.delete()
        contents?.delete()
    }

    def "test withDir_Path"() {
        given:
        def path
        def contents
        def ret
        def e

        when: 'The closure completes successfully. Delete contents only.'
        path = Files.createTempDirectory('tst')
        contents = Files.createTempFile(path, 'tst', null)
        ret = path.withDir(true) { return 1 }

        then: 'The return value is that of the closure and the directory contents only has been deleted'
        ret == 1
        Files.exists(path)
        Files.notExists(contents)

        when: 'The closure completes successfully'
        Files.createTempFile(path, 'tst', null)
        ret = path.withDir { return 1 }

        then: 'The return value is that of the closure and the directory has been deleted'
        ret == 1
        Files.notExists(path)

        when: 'The closure throws an exception. Delete contents only.'
        path = Files.createTempDirectory('tst')
        contents = Files.createTempFile(path, 'tst', null)
        path.withDir(true) { throw new IndexOutOfBoundsException() }

        then: 'The exception is propagated and the directory contents only has been deleted'
        thrown(IndexOutOfBoundsException)
        Files.exists(path)
        Files.notExists(contents)

        when: 'The closure throws an exception'
        Files.createTempFile(path, 'tst', null)
        path.withDir { throw new IndexOutOfBoundsException() }

        then: 'The exception is propagated and the directory has been deleted'
        thrown(IndexOutOfBoundsException)
        Files.notExists(path)

        when: 'The closure completes successfully, but the directory does not exist'
        ret = path.withDir { return 2 }

        then: 'The return value is that of the closure and no exception is thrown when trying to delete the directory'
        notThrown(IOException)
        ret == 2

        when: 'The closure throws an exception and the directory does not exist'
        path.withDir { throw new IndexOutOfBoundsException() }

        then: 'The exception is propagated and no exception is suppressed when trying to delete the directory'
        e = thrown(IndexOutOfBoundsException)
        e.suppressed.length == 0

        when: 'The closure completes successfully, but the path is not a directory'
        path = Files.createTempFile('tst', null)
        path.withDir { return 3 }

        then: 'The IllegalArgumentException thrown when trying to delete the directory is propagated'
        thrown(IllegalArgumentException)

        when: 'The closure throws an exception and the path is not a directory'
        path.withDir { throw new IndexOutOfBoundsException() }

        then: 'The exception is propagated and IllegalArgumentException is suppressed when trying to delete the directory'
        e = thrown(IndexOutOfBoundsException)
        e.suppressed[0] instanceof IllegalArgumentException

        cleanup:
        if (path) Files.deleteIfExists(path)
        if (contents) Files.deleteIfExists(contents)
    }

    def "test initDir_Path"() {
        given:
        def path
        def contents
        def ret
        def e

        when: 'The closure completes successfully'
        path = Files.createTempDirectory('tst')
        contents = Files.createTempFile(path, 'tst', null)
        ret = path.initDir { return 1 }

        then: 'The return value is that of the closure and neither the directory not its contents have been deleted'
        ret == 1
        Files.exists(path)
        Files.exists(contents)

        when: 'The closure throws an exception. Delete contents only.'
        path.initDir(true) { throw new IndexOutOfBoundsException() }

        then: 'The exception is propagated and the directory contents only has been deleted'
        thrown(IndexOutOfBoundsException)
        Files.exists(path)
        Files.notExists(contents)

        when: 'The closure throws an exception'
        Files.createTempFile(path, 'tst', null)
        path.initDir { throw new IndexOutOfBoundsException() }

        then: 'The exception is propagated and the directory has been deleted'
        thrown(IndexOutOfBoundsException)
        Files.notExists(path)

        when: 'The closure throws an exception and the directory does not exist'
        path.initDir { throw new IndexOutOfBoundsException() }

        then: 'The exception is propagated and no exception is suppressed when trying to delete the directory'
        e = thrown(IndexOutOfBoundsException)
        e.suppressed.length == 0

        when: 'The closure throws an exception and the path is not a directory'
        path = Files.createTempFile('tst', null)
        path.initDir { throw new IndexOutOfBoundsException() }

        then: 'The exception is propagated and IllegalArgumentException is suppressed when trying to delete the directory'
        e = thrown(IndexOutOfBoundsException)
        e.suppressed[0] instanceof IllegalArgumentException

        cleanup:
        Files.deleteIfExists(path)
    }

    def "test withResource"() {
        given:
        def closeable = new MyCloseable()
        def aList
        def ret

        when: 'The closure returns successfully and no explicit cleanup is specified'
        closeable.reset()
        ret = closeable.withResource {return 1 }

        then: 'The return value is that of the closure and the close method was called'
        ret == 1
        closeable.isClosed()

        when: 'The closure throws an exception and no explicit cleanup is specified'
        closeable.reset()
        closeable.withResource {throw new IndexOutOfBoundsException() }

        then: 'The exception is propagated and the close method was called'
        thrown(IndexOutOfBoundsException)
        closeable.isClosed()

        when: 'The closure returns successfully and a explicit cleanup is specified'
        closeable.close()
        ret = closeable.withResource({ it.reset() }) {return 2 }

        then: 'The return value is that of the closure and the given cleanup was executed'
        ret == 2
        !closeable.isClosed()

        when: 'The closure throws an exception and a explicit cleanup is specified'
        closeable.close()
        closeable.withResource({ it.reset() }) {throw new IndexOutOfBoundsException() }

        then: 'The exception is propagated and the given cleanup was executed'
        thrown(IndexOutOfBoundsException)
        !closeable.isClosed()

        when: 'The closure returns successfully, but the cleanup fails'
        closeable.reset()
        closeable.withResource({ it.fail() }) {return 3 }

        then: 'The exception thrown by the cleanup is propagated'
        thrown(UnsupportedOperationException)
        !closeable.isClosed()

        when: 'The closure throws an exception and the cleanup fails'
        closeable.reset()
        closeable.withResource({ it.fail() }) {
            throw new IndexOutOfBoundsException()
        }

        then: 'The exception is propagated and the cleanup exception is suppressed'
        def e = thrown(IndexOutOfBoundsException)
        e.suppressed[0] instanceof UnsupportedOperationException
        !closeable.isClosed()

        when: 'Specific cleanup with a resource that has no close method. The closure succeeds.'
        aList = ['test']
        ret = aList.withResource({ it.clear() }) {
            return it.size()
        }

        then: 'The returned value is that of the closure and the cleanup was run'
        ret == 1
        aList.isEmpty()

        when: 'Specific cleanup with a resource that has no close method. The closure throws an exception.'
        aList = ['test']
        aList.withResource({ it.clear() }) {
            throw new IndexOutOfBoundsException()
        }

        then: 'The exception is propagated and the cleanup was run'
        thrown(IndexOutOfBoundsException)
        aList.isEmpty()
    }

    def "test initResource"() {
        given:
        def closeable = new MyCloseable()
        def aList
        def ret

        when: 'The closure returns successfully and no explicit cleanup is specified'
        closeable.reset()
        ret = closeable.initResource {return 1 }

        then: 'The return value is that of the closure and the close method was not called'
        ret == 1
        !closeable.isClosed()

        when: 'The closure throws an exception and no explicit cleanup is specified'
        closeable.reset()
        closeable.initResource {throw new IndexOutOfBoundsException() }

        then: 'The exception is propagated and the close method was called'
        thrown(IndexOutOfBoundsException)
        closeable.isClosed()

        when: 'The closure returns successfully and a explicit cleanup is specified'
        closeable.close()
        ret = closeable.initResource({ it.reset() }) {return 2 }

        then: 'The return value is that of the closure and the given cleanup was not executed'
        ret == 2
        closeable.isClosed()

        when: 'The closure throws an exception and a explicit cleanup is specified'
        closeable.close()
        closeable.initResource({ it.reset() }) {throw new IndexOutOfBoundsException() }

        then: 'The exception is propagated and the given cleanup was executed'
        thrown(IndexOutOfBoundsException)
        !closeable.isClosed()

        when: 'The closure returns successfully, but the cleanup fails'
        closeable.reset()
        ret = closeable.initResource({ it.fail() }) {return 3 }

        then: 'No exception was thrown as the cleanup was not run and the returned value is that of the closure'
        notThrown(UnsupportedOperationException)
        ret == 3
        !closeable.isClosed()

        when: 'The closure throws an exception and the cleanup fails'
        closeable.reset()
        closeable.initResource ({ it.fail() }) {
            throw new IndexOutOfBoundsException()
        }

        then: 'The exception is propagated and the cleanup exception is suppressed'
        def e = thrown(IndexOutOfBoundsException)
        e.suppressed[0] instanceof UnsupportedOperationException
        !closeable.isClosed()

        when: 'Specific cleanup with a resource that has no close method. The closure succeeds.'
        aList = ['test']
        ret = aList.initResource({ it.clear() }) {
            return it.size()
        }

        then: 'The returned value is that of the closure and the cleanup was not run'
        ret == 1
        aList[0] == 'test'

        when: 'Specific cleanup with a resource that has no close method. The closure throws an exception.'
        aList = ['test']
        aList.initResource({ it.clear() }) {
            throw new IndexOutOfBoundsException()
        }

        then: 'The exception is propagated and the cleanup was run'
        thrown(IndexOutOfBoundsException)
        aList.isEmpty()
    }

    private static class MyCloseable {

        private closed = false

        boolean isClosed() {
            return closed
        }

        void close() {
            closed = true
        }

        void reset() {
            closed = false
        }

        @SuppressWarnings('GrMethodMayBeStatic')
        void fail() {
            throw new UnsupportedOperationException()
        }

    }

}
