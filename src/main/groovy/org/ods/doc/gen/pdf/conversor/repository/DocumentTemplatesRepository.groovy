package org.ods.doc.gen.pdf.conversor.repository


import org.springframework.stereotype.Repository

import javax.cache.annotation.CacheKey
import javax.cache.annotation.CacheResult
import java.nio.file.Path

@Repository
interface DocumentTemplatesRepository {

    @CacheResult(cacheName = "templates")
    Path getTemplatesForVersion(@CacheKey String version)
    
    boolean isApplicableToSystemConfig()

    URI getURItoDownloadTemplates(String version)
}
