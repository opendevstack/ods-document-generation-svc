package org.ods.doc.gen.pdf.builder.repository

import feign.FeignException
import feign.Request
import feign.RequestTemplate
import org.ods.doc.gen.BitBucketClientConfig
import org.ods.doc.gen.core.ZipFacade
import spock.lang.Specification
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables

class BitBucketDocumentTemplatesRepositorySpec extends Specification {

    def "error msg in request by #exceptionTypeName"(){
        given:
        def version = "1.0"
        def repository = new BitBucketDocumentTemplatesRepository(
                new BitBucketClientConfig(),
                new ZipFacade(),
                "basePath")
        def uri = BitBucketDocumentTemplatesRepository.getURItoDownloadTemplates(version)
        def store = Mock(BitBucketDocumentTemplatesStoreHttpAPI)
        store.getTemplatesZipArchiveForVersion(_, _, _) >> { throw createException(exceptionTypeName)}

        when:
        repository.getTemplatesForVersion(version)

        then:
        def e = thrown(RuntimeException)

        where:
        exceptionTypeName << ["BadRequest", "Unauthorized", "NotFound", "Other"]
    }

    private FeignException createException(String exceptionTypeName) {
        def headers = ["a":"aa"]
        def request = Request.create(Request.HttpMethod.GET, "url", headers, Request.Body.create("h"), new RequestTemplate())
        def feignException
        switch(exceptionTypeName) {
            case "BadRequest":
                feignException = new FeignException.BadRequest("BadRequest", request, null, null)
                break
            case "Unauthorized":
                feignException = new FeignException.Unauthorized("BadRequest", request, null, null)
                break
            case "NotFound":
                feignException = new FeignException.NotFound("BadRequest", request, null, null)
                break
            default:
                feignException = new FeignException(500, "")
        }
        return feignException
    }
}
