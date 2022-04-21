package org.ods.doc.gen.leva.doc.test.doubles

import org.ods.doc.gen.BitBucketClientConfig
import org.ods.doc.gen.adapters.git.BitbucketService
import org.ods.doc.gen.core.ZipFacade
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

import javax.inject.Inject

@Profile("test")
@Service
class BitbucketServiceMock extends BitbucketService {

    @Inject
    BitbucketServiceMock(BitBucketClientConfig bitBucketClientConfig, ZipFacade zipFacade) {
        super(bitBucketClientConfig, zipFacade)
    }

    @Override
    String getBitbucketURLForDocs() {
        return "http://wiremockTest"
    }

}
