package org.ods.doc.gen.templates.repository


import feign.FeignException
import feign.Request
import feign.RequestTemplate
import org.springframework.core.env.Environment
import spock.lang.Specification
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables

class BitBucketDocumentTemplatesRepositorySpec extends Specification {

    EnvironmentVariables env = new EnvironmentVariables()

    def setup(){
        env.setup()
        env.set("BITBUCKET_DOCUMENT_TEMPLATES_PROJECT", "myProject")
        env.set("BITBUCKET_DOCUMENT_TEMPLATES_REPO", "myRepo")
        env.set("BITBUCKET_URL", "http://localhost:9001")
    }

    def cleanup(){
        env.teardown()
    }

    def "error msg in request by #exceptionTypeName"(){
        given:
        def version = "1.0"
        def environment = Mock(Environment)
        def repository = new BitBucketDocumentTemplatesRepository(null, environment)
        def uri = repository.getURItoDownloadTemplates(version)
        def store = Mock(BitBucketDocumentTemplatesStoreHttpAPI)
        store.getTemplatesZipArchiveForVersion(_, _, _) >> { throw createException(exceptionTypeName)}

        when:
        repository.getZipArchive(store, version, uri, "bitbucketUserName")

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
