package org.ods.doc.gen.core

import org.springframework.stereotype.Service

import java.nio.file.Files
import java.nio.file.Path

@Service
class FileSystemHelper {

    Path createTempDirectory(String id){
        return Files.createTempDirectory(id)
    }

}
