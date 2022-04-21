package org.ods.doc.gen.pdf.builder.repository

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Repository

import java.nio.file.Path

@Repository
interface DocumentTemplatesRepository {

    @Cacheable(value = "templates", key = '#version')
    Path getTemplatesForVersion(String version)
    
    boolean isApplicableToSystemConfig()

}
