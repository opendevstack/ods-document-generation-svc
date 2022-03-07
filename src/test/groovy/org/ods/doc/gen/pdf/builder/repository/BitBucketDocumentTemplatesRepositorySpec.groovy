package org.ods.doc.gen.pdf.builder.repository

import feign.FeignException
import feign.Request
import feign.RequestTemplate
import org.ods.doc.gen.BitBucketClientConfig
import org.ods.doc.gen.core.ZipFacade
import org.ods.doc.gen.external.modules.git.BitbucketService
import org.springframework.beans.factory.annotation.Value
import spock.lang.Specification
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables

import javax.inject.Inject

class BitBucketDocumentTemplatesRepositorySpec extends Specification {

    @Value('${bitbucket.username}')
    String bitbucketUsername
    @Value('${bitbucket.password}')
    String bitbucketPassword
    @Value('${bitbucket.url}')
    String bitbucketUrl

    def "error msg in request by #exceptionTypeName"(){
        given:
        def version = "1.0"
        String bbDocProject = "?"
        String bbRepo = "?"
        ZipFacade zipFacade = new ZipFacade()
        BitbucketService bitbucketService = Spy(new BitbucketService(
                new BitBucketClientConfig(bitbucketUsername, bitbucketPassword, bitbucketUrl),
                zipFacade
        ))
        def repository = new BitBucketDocumentTemplatesRepository(
                bitbucketService,
                zipFacade,
                "basePath", bbDocProject, bbRepo)

        bitbucketService.downloadRepo(_,_,_,_) >> { throw createException(exceptionTypeName)}
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
