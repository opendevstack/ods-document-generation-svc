package org.ods.doc.gen.core

import org.springframework.stereotype.Service

import java.nio.file.Files
import java.nio.file.Path

@Service
class FileSystemHelper {

    Path createTempDirectory(String id){
        return Files.createTempDirectory(id)
    }

    List<File> loadFilesFromPath(String path, String fileExtension) {
        def result = []
        try {
            new File(path).traverse(nameFilter: ~/.*\.${fileExtension}$/, type: groovy.io.FileType.FILES) { file ->
                result << file
            }
        } catch (FileNotFoundException e) {}

        return result
    }
}
