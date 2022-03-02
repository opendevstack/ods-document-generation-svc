package org.ods.doc.gen.pdf.builder.repository

import feign.FeignException
import feign.Headers
import feign.Param
import feign.RequestLine
import feign.Response
import feign.codec.ErrorDecoder
import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.apache.http.client.utils.URIBuilder
import org.ods.doc.gen.BitBucketClientConfig
import org.ods.doc.gen.GithubClientConfig
import org.ods.doc.gen.core.ZipFacade
import org.ods.doc.gen.external.modules.git.GithubService
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Repository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

import javax.inject.Inject
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

@Slf4j
@Order(1)
@Repository
class GithubDocumentTemplatesRepository implements DocumentTemplatesRepository {

    private final ZipFacade zipFacade
    private final String basePath
    private final GithubService githubService

    @Inject
    GithubDocumentTemplatesRepository(GithubService githubService,
                                      ZipFacade zipFacade,
                                      @Value('${cache.documents.basePath}') String basePath){
        this.githubService = githubService
        this.zipFacade = zipFacade
        this.basePath = basePath
    }

    Path getTemplatesForVersion(String version) {
        log.info ("getTemplatesForVersion version:${version}")

        def targetDir = Paths.get(basePath, version)
        githubService.downloadRepo(version, targetDir)
        moveContentToRootFolder(targetDir, "ods-document-generation-templates-${version}")
        return targetDir
    }

    boolean isApplicableToSystemConfig () {
        return true
    }

    private void moveContentToRootFolder(targetDir, startAtDir){
        FileUtils.copyDirectory(new File(targetDir.toFile(), startAtDir), targetDir.toFile())
        FileUtils.deleteDirectory(new File(targetDir.toFile(), startAtDir))
    }

}
