package util

import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.attribute.FileAttribute

class FileToolsSpec extends Specification {

    def "test withTempFile with parent"() {
        given:
        def parent = Files.createTempDirectory('tst')
        def prefix = 'tst'
        def suffix = '.ext'
        def attrs = []
        GroovySpy(Files, global: true)
        def ret
        def tempFile = null

        when: 'The closure succeeds'
        ret = FileTools.withTempFile(parent, prefix, suffix, attrs) { return [it] }

        then: 'The temp file was created with Files and has been deleted'
        1 * Files.createTempFile(parent, prefix, suffix, { FileAttribute<?>[] varArgs ->
            Arrays.equals(varArgs, attrs as FileAttribute<?>[])
        }) >> { tempFile = callRealMethod() }
        ret[0].is(tempFile)
        Files.notExists(tempFile)

        when: 'The closure throws an exception'
        FileTools.withTempFile(parent, prefix, suffix, attrs) { throw new RuntimeException() }

        then: 'A TempFile was created with Files and deleted afterwards. The exception is propagated.'
        1 * Files.createTempFile(parent, prefix, suffix, { FileAttribute<?>[] varArgs ->
            Arrays.equals(varArgs, attrs as FileAttribute<?>[])
        }) >> { tempFile = callRealMethod() }
        Files.notExists(tempFile)
        thrown(RuntimeException)

        cleanup:
        parent.deleteDir()
        Files.deleteIfExists(tempFile)
    }

    def "test withTempFile without parent"() {
        given:
        def prefix = 'tst'
        def suffix = '.ext'
        def attrs = []
        GroovySpy(Files, global: true)
        def ret
        def tempFile = null

        when: 'The closure succeeds'
        ret = FileTools.withTempFile(prefix, suffix, attrs) { return [it] }

        then: 'The temp file was created with Files and has been deleted'
        1 * Files.createTempFile(prefix, suffix, { FileAttribute<?>[] varArgs ->
            Arrays.equals(varArgs, attrs as FileAttribute<?>[])
        }) >> { tempFile = callRealMethod() }
        ret[0].is(tempFile)
        Files.notExists(tempFile)

        when: 'The closure throws an exception'
        FileTools.withTempFile(prefix, suffix, attrs) { throw new RuntimeException() }

        then: 'A TempFile was created with Files and deleted afterwards. The exception is propagated.'
        1 * Files.createTempFile(prefix, suffix, { FileAttribute<?>[] varArgs ->
            Arrays.equals(varArgs, attrs as FileAttribute<?>[])
        }) >> { tempFile = callRealMethod() }
        Files.notExists(tempFile)
        thrown(RuntimeException)

        cleanup:
        Files.deleteIfExists(tempFile)
    }

    def "test newTempFile with parent"() {
        given:
        def parent = Files.createTempDirectory('tst')
        def prefix = 'tst'
        def suffix = '.ext'
        def attrs = []
        GroovySpy(Files, global: true)
        def ret
        def tempFile = null
        def closureArg = null

        when: 'The closure throws an exception'
        FileTools.newTempFile(parent, prefix, suffix, attrs) { closureArg = it; throw new RuntimeException() }

        then: 'A TempFile was created with Files and deleted afterwards. The exception is propagated.'
        1 * Files.createTempFile(parent, prefix, suffix, { FileAttribute<?>[] varArgs ->
            Arrays.equals(varArgs, attrs as FileAttribute<?>[])
        }) >> { tempFile = callRealMethod() }
        closureArg.is(tempFile)
        Files.notExists(tempFile)
        thrown(RuntimeException)

        when: 'The closure succeeds'
        ret = FileTools.newTempFile(parent, prefix, suffix, attrs) { closureArg = it; return null }

        then: 'The temp file was created with Files and has been deleted'
        1 * Files.createTempFile(parent, prefix, suffix, { FileAttribute<?>[] varArgs ->
            Arrays.equals(varArgs, attrs as FileAttribute<?>[])
        }) >> { tempFile = callRealMethod() }
        ret.is(tempFile)
        closureArg.is(tempFile)
        Files.exists(tempFile)

        cleanup:
        parent.deleteDir()
        Files.deleteIfExists(tempFile)
    }

    def "test newTempFile without parent"() {
        given:
        def prefix = 'tst'
        def suffix = '.ext'
        def attrs = []
        GroovySpy(Files, global: true)
        def ret
        def tempFile = null
        def closureArg = null

        when: 'The closure throws an exception'
        FileTools.newTempFile(prefix, suffix, attrs) { closureArg = it; throw new RuntimeException() }

        then: 'A TempFile was created with Files and deleted afterwards. The exception is propagated.'
        1 * Files.createTempFile(prefix, suffix, { FileAttribute<?>[] varArgs ->
            Arrays.equals(varArgs, attrs as FileAttribute<?>[])
        }) >> { tempFile = callRealMethod() }
        closureArg.is(tempFile)
        Files.notExists(tempFile)
        thrown(RuntimeException)

        when: 'The closure succeeds'
        ret = FileTools.newTempFile(prefix, suffix, attrs) { closureArg = it; return null }

        then: 'The temp file was created with Files and has been deleted'
        1 * Files.createTempFile(prefix, suffix, { FileAttribute<?>[] varArgs ->
            Arrays.equals(varArgs, attrs as FileAttribute<?>[])
        }) >> { tempFile = callRealMethod() }
        ret.is(tempFile)
        closureArg.is(tempFile)
        Files.exists(tempFile)

        cleanup:
        Files.deleteIfExists(tempFile)
    }

    def "test withTempDir with parent"() {
        given:
        def parent = Files.createTempDirectory('tst')
        def prefix = 'tst'
        def attrs = []
        GroovySpy(Files, global: true)
        def ret
        def tempFile = null

        when: 'The closure succeeds'
        ret = FileTools.withTempDir(parent, prefix, attrs) { return [it] }

        then: 'The temp file was created with Files and has been deleted'
        1 * Files.createTempDirectory(parent, prefix, { FileAttribute<?>[] varArgs ->
            Arrays.equals(varArgs, attrs as FileAttribute<?>[])
        }) >> { tempFile = callRealMethod() }
        ret[0].is(tempFile)
        Files.notExists(tempFile)

        when: 'The closure throws an exception'
        FileTools.withTempDir(parent, prefix, attrs) { throw new RuntimeException() }

        then: 'A TempFile was created with Files and deleted afterwards. The exception is propagated.'
        1 * Files.createTempDirectory(parent, prefix, { FileAttribute<?>[] varArgs ->
            Arrays.equals(varArgs, attrs as FileAttribute<?>[])
        }) >> { tempFile = callRealMethod() }
        Files.notExists(tempFile)
        thrown(RuntimeException)

        cleanup:
        parent.deleteDir()
    }

    def "test withTempDir without parent"() {
        given:
        def prefix = 'tst'
        def attrs = []
        GroovySpy(Files, global: true)
        def ret
        def tempFile = null

        when: 'The closure succeeds'
        ret = FileTools.withTempDir(prefix, attrs) { return [it] }

        then: 'The temp file was created with Files and has been deleted'
        1 * Files.createTempDirectory(prefix, { FileAttribute<?>[] varArgs ->
            Arrays.equals(varArgs, attrs as FileAttribute<?>[])
        }) >> { tempFile = callRealMethod() }
        ret[0].is(tempFile)
        Files.notExists(tempFile)

        when: 'The closure throws an exception'
        FileTools.withTempDir(prefix, attrs) { throw new RuntimeException() }

        then: 'A TempFile was created with Files and deleted afterwards. The exception is propagated.'
        1 * Files.createTempDirectory(prefix, { FileAttribute<?>[] varArgs ->
            Arrays.equals(varArgs, attrs as FileAttribute<?>[])
        }) >> { tempFile = callRealMethod() }
        Files.notExists(tempFile)
        thrown(RuntimeException)

        cleanup:
        Files.deleteIfExists(tempFile)
    }

    def "test newTempDir with parent"() {
        given:
        def parent = Files.createTempDirectory('tst')
        def prefix = 'tst'
        def attrs = []
        GroovySpy(Files, global: true)
        def ret
        def tempFile = null
        def closureArg = null

        when: 'The closure succeeds'
        ret = FileTools.newTempDir(parent, prefix, attrs) { closureArg = it; return null }

        then: 'The temp file was created with Files and has been deleted'
        1 * Files.createTempDirectory(parent, prefix, { FileAttribute<?>[] varArgs ->
            Arrays.equals(varArgs, attrs as FileAttribute<?>[])
        }) >> { tempFile = callRealMethod() }
        ret.is(tempFile)
        closureArg.is(tempFile)
        Files.exists(tempFile)

        when: 'The closure throws an exception'
        FileTools.newTempDir(parent, prefix, attrs) { closureArg = it; throw new RuntimeException() }

        then: 'A TempFile was created with Files and deleted afterwards. The exception is propagated.'
        1 * Files.createTempDirectory(parent, prefix, { FileAttribute<?>[] varArgs ->
            Arrays.equals(varArgs, attrs as FileAttribute<?>[])
        }) >> { tempFile = callRealMethod() }
        closureArg.is(tempFile)
        Files.notExists(tempFile)
        thrown(RuntimeException)

        cleanup:
        parent.deleteDir()
    }

    def "test newTempDir without parent"() {
        given:
        def prefix = 'tst'
        def attrs = []
        GroovySpy(Files, global: true)
        def ret
        def tempFile = null
        def closureArg = null

        when: 'The closure succeeds'
        ret = FileTools.newTempDir(prefix, attrs) { closureArg = it; return null }

        then: 'The temp file was created with Files and has been deleted'
        1 * Files.createTempDirectory(prefix, { FileAttribute<?>[] varArgs ->
            Arrays.equals(varArgs, attrs as FileAttribute<?>[])
        }) >> { tempFile = callRealMethod() }
        ret.is(tempFile)
        closureArg.is(tempFile)
        Files.exists(tempFile)

        when: 'The closure throws an exception'
        FileTools.newTempDir(prefix, attrs) { closureArg = it; throw new RuntimeException() }

        then: 'A TempFile was created with Files and deleted afterwards. The exception is propagated.'
        1 * Files.createTempDirectory(prefix, { FileAttribute<?>[] varArgs ->
            Arrays.equals(varArgs, attrs as FileAttribute<?>[])
        }) >> { tempFile = callRealMethod() }
        closureArg.is(tempFile)
        Files.notExists(tempFile)
        thrown(RuntimeException)

        cleanup:
        Files.deleteIfExists(tempFile)
    }

}
