package org.ods.doc.gen.leva.doc.services

import groovy.util.logging.Slf4j
import org.ods.shared.lib.project.data.ProjectData
import org.springframework.stereotype.Service

import java.nio.file.Paths

import org.yaml.snakeyaml.Yaml

@Slf4j
@Service
class LeVADocumentChaptersFileService {

    static final String DOCUMENT_CHAPTERS_BASE_DIR = 'docs'

    Map getDocumentChapterData(ProjectData projectData, String documentType) {
        if (!documentType?.trim()) {
            throw new IllegalArgumentException(
                "Error: unable to load document chapters. 'documentType' is undefined."
            )
        }

        String yamlText
        def file = Paths.get(projectData.data.env.WORKSPACE as String, DOCUMENT_CHAPTERS_BASE_DIR, "${documentType}.yaml").toFile()
        if (!file.exists()) {
            try {
                yamlText = new File("${projectData.data.env.WORKSPACE}/docs/${documentType}.yaml")?.text
            } catch(Exception exception){
                yamlText = "" // TODO s2o throw exception??
            }
        } else {
            yamlText = file.text
        }

        def data = [:]
        if (!yamlText) {
            throw new RuntimeException(
                "Error: unable to load document chapters. File 'docs/${documentType}.yaml' could not be read."
            )
        } else {
            data = new Yaml().load(yamlText) ?: [:]
        }

        return data.collectEntries { chapter ->
            def number = chapter.number.toString()
            chapter.number = number
            chapter.content = chapter.content ?: ''
            chapter['status'] = 'done'
            [ "sec${number.replaceAll(/\./, 's')}".toString(), chapter ]
        }
    }

}
