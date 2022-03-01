package org.ods.doc.gen.pdf.builder.repository

import groovy.util.logging.Slf4j
import org.ods.doc.gen.core.ZipFacade
import org.ods.doc.gen.external.modules.git.BitbucketService
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Repository

import javax.inject.Inject
import java.nio.file.Path
import java.nio.file.Paths

@Slf4j
@Order(0)
@Repository
class BitBucketDocumentTemplatesRepository implements DocumentTemplatesRepository {

    private final ZipFacade zipFacade
    private final String basePath
    private final BitbucketService bitbucketService

    @Inject
    BitBucketDocumentTemplatesRepository(BitbucketService bitbucketService,
                                         ZipFacade zipFacade,
                                         @Value('${cache.documents.basePath}') String basePath){
        this.bitbucketService = bitbucketService
        this.basePath = basePath
        this.zipFacade = zipFacade
    }

    Path getTemplatesForVersion(String version) {
        log.info ("getTemplatesForVersion version:${version}")
        String project = System.getenv("BITBUCKET_DOCUMENT_TEMPLATES_PROJECT")
        String repo = System.getenv("BITBUCKET_DOCUMENT_TEMPLATES_REPO")
        Path targetDir = Paths.get(basePath, version)
        bitbucketService.downloadRepo(project, repo, "refs/heads/release/v${version}", targetDir.toString())
        return targetDir
    }

    boolean isApplicableToSystemConfig () {
        List missingEnvs = [ ]
        if (!System.getenv("BITBUCKET_URL")) {
            missingEnvs << "BITBUCKET_URL"
        }
        if (!System.getenv("BITBUCKET_DOCUMENT_TEMPLATES_PROJECT")) {
            missingEnvs << "BITBUCKET_DOCUMENT_TEMPLATES_PROJECT"
        }
        if (!System.getenv("BITBUCKET_DOCUMENT_TEMPLATES_REPO")) {
            missingEnvs << "BITBUCKET_DOCUMENT_TEMPLATES_REPO"
        }
        if (missingEnvs.size() > 0) {
            log.warn "Bitbucket adapter not applicable - missing config '${missingEnvs}'"
            return false
        }

        return true
    }

}
