package org.ods.doc.gen.core

import groovy.io.FileType
import groovy.util.logging.Slf4j
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

import java.nio.file.Files
import java.nio.file.Path

@Slf4j
@Service
class FileSystemHelper {

    @Cacheable(value = "temporalFolder", key = '#id')
    Path createTempDirectory(String id){
        return Files.createTempDirectory(id)
    }

    List<File> loadFilesFromPath(String path, String fileExtension) {
        def result = []
        try {
            new File(path).traverse(nameFilter: ~/.*\.${fileExtension}$/, type: FileType.FILES) { file ->
                result << file
            }
        } catch (FileNotFoundException e) {
            log.warn("No file of type ${fileExtension}, found here:${path}")
        }

        return result
    }

}
