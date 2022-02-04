package org.ods.doc.gen.project.data

import feign.Feign
import feign.FeignException
import feign.Headers
import feign.Param
import feign.RequestLine
import feign.auth.BasicAuthRequestInterceptor
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Service

@SuppressWarnings(['LineLength',
        'AbcMetric',
        'IfStatementBraces',
        'Instanceof',
        'CyclomaticComplexity',
        'GStringAsMapKey',
        'ImplementationAsType',
        'UseCollectMany',
        'MethodCount',
        'PublicMethodsBeforeNonPublicMethods'])

interface BitBucketReleaseManagerRepoHttpAPI {
    @Headers("Accept: application/octet-stream")
    // TODO: Fix url
    @RequestLine("GET /rest/api/latest/projects/{documentTemplatesProject}/repos/{documentTemplatesRepo}/archive?at=refs/heads/release/v{version}&format=zip")
    byte[] getRepoBranchInZipArchive(@Param("documentTemplatesProject") String documentTemplatesProject, @Param("documentTemplatesRepo") String documentTemplatesRepo, @Param("version") String version)
}

@Slf4j
@Service
class ReleaseManagerRepo {

    ReleaseManagerRepo(ProjectData projectData) {

        byte[] zipArchiveContent = getZipArchive(store, version, uri, bitbucketUserName)
        zipFacade.extractZipArchive(zipArchiveContent, targetDir)
    }


    private byte[] getZipArchive(BitBucketDocumentTemplatesStoreHttpAPI store, String version, uri, String bitbucketUserName) {
        def bitbucketRepo = System.getenv("BITBUCKET_DOCUMENT_TEMPLATES_REPO")
        def bitbucketProject = System.getenv("BITBUCKET_DOCUMENT_TEMPLATES_PROJECT")
        try {
            return store.getTemplatesZipArchiveForVersion(bitbucketProject, bitbucketRepo, version)
        } catch (FeignException callException) {
            def baseErrMessage = "Could not get document zip from '${uri}'!"
            def baseRepoErrMessage = "${baseErrMessage}\rIn repository '${bitbucketRepo}' - "
            if (callException instanceof FeignException.BadRequest) {
                throw new RuntimeException("${baseRepoErrMessage}" +
                        "is there a correct release branch configured, called 'release/v${version}'?")
            } else if (callException instanceof FeignException.Unauthorized) {
                def bbUserNameError = bitbucketUserName ?: 'Anyone'
                throw new RuntimeException("${baseRepoErrMessage} \rDoes '${bbUserNameError}' have access?")
            } else if (callException instanceof FeignException.NotFound) {
                throw new RuntimeException("${baseErrMessage}" +
                        "\rDoes repository '${bitbucketRepo}' in project: '${bitbucketProject}' exist?")
            } else {
                throw callException
            }
        }
    }

    private BitBucketDocumentTemplatesStoreHttpAPI createStorageClient(String bitbucketUserName, String bitbucketPassword, URI uri) {
        Feign.Builder builder = Feign.builder()
        if (bitbucketUserName && bitbucketPassword) {
            builder.requestInterceptor(new BasicAuthRequestInterceptor(bitbucketUserName, bitbucketPassword))
        }

        return builder.target(BitBucketDocumentTemplatesStoreHttpAPI.class, uri.getScheme() + "://" + uri.getAuthority())
    }

}
