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

    public static final String BRANCH = "refs/heads/release/v"
    public static final String NONE = "none"

    private final ZipFacade zipFacade
    private final String basePath
    private final BitbucketService bitbucketService
    String bbDocProject
    String bbRepo

    @Inject
    BitBucketDocumentTemplatesRepository(BitbucketService bitbucketService,
                                         ZipFacade zipFacade,
                                         @Value('${cache.documents.basePath}') String basePath,
                                         @Value('${bitbucket.documents.project}') String bbDocProject,
                                         @Value('${bitbucket.documents.repo}') String bbRepo){
        this.bitbucketService = bitbucketService
        this.basePath = basePath
        this.zipFacade = zipFacade
        this.bbDocProject = bbDocProject
        this.bbRepo = bbRepo
    }

    Path getTemplatesForVersion(String version) {
        log.info ("getTemplatesForVersion version:${version}")
        Path targetDir = Paths.get(basePath, version)
        if (!targetDir.toFile().exists()) {
            bitbucketService.downloadRepo(bbDocProject, bbRepo, "${BRANCH}${version}", targetDir.toString())
        }
        return targetDir
    }

    boolean isApplicableToSystemConfig () {
        if (bbDocProject== NONE) {
            log.warn "Bitbucket adapter not applicable. If needed review BITBUCKET env params - Using Github'"
            return false
        }

        return true
    }

}
