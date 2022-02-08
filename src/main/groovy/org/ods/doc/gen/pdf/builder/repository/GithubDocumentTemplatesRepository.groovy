package org.ods.doc.gen.pdf.builder.repository

import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.ods.doc.gen.core.ZipFacade
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Repository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

import javax.inject.Inject
import java.nio.file.Path
import java.nio.file.Paths

@Repository
@FeignClient(name = "github-client", url = "https://www.github.com")
interface GithubDocumentTemplatesStoreHttpAPI {

  @GetMapping("/opendevstack/ods-document-generation-templates/archive/v{version}.zip")
  byte[] getTemplatesZipArchiveForVersion(@PathVariable("version") String version)

}

@Slf4j
@Order(1)
@Repository
class GithubDocumentTemplatesRepository implements DocumentTemplatesRepository {

    private final ZipFacade zipFacade
    private final String basePath
    private final GithubDocumentTemplatesStoreHttpAPI store

    @Inject
    GithubDocumentTemplatesRepository(GithubDocumentTemplatesStoreHttpAPI store, ZipFacade zipFacade, @Value('${cache.documents.basePath}') String basePath){
        this.store = store
        this.zipFacade = zipFacade
        this.basePath = basePath
    }

    Path getTemplatesForVersion(String version) {
        log.info ("getTemplatesForVersion version:${version}")

        def targetDir = Paths.get(basePath, version)
        def zipContent = store.getTemplatesZipArchiveForVersion(version)
        zipFacade.extractZipArchive(zipContent, targetDir)
        moveContentToRootFolder(targetDir, "ods-document-generation-templates-${version}")
        return targetDir
    }

    boolean isApplicableToSystemConfig () {
        return true
    }

    URI getURItoDownloadTemplates(String version) {
        return null
    }

    private void moveContentToRootFolder(targetDir, startAtDir){
        FileUtils.copyDirectory(new File(targetDir.toFile(), startAtDir), targetDir.toFile())
        FileUtils.deleteDirectory(new File(targetDir.toFile(), startAtDir))
    }
}
