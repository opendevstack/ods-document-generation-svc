package org.ods.doc.gen.leva.doc.repositories

import groovy.json.JsonSlurperClassic

import java.nio.file.NoSuchFileException

class ProjectDataRepository {

    static final String BASE_DIR = 'projectData'

    static Object loadFile(String tmpFolder, String savedVersion) {
        String fileName = "${tmpFolder}/${BASE_DIR}/${savedVersion}.json"
        File savedData =  new File(fileName)
        def data = [:]
        if (!savedData.exists()) {
            throw new NoSuchFileException("File '${fileName}.json' is expected to be inside the release " +
                    'manager repository but was not found and thus, document history cannot be build. If you come from ' +
                    'and old ODS version, create one for each document to use the automated document history feature.')
        }
        return new JsonSlurperClassic().parse(savedData)?: [:]
    }

}
