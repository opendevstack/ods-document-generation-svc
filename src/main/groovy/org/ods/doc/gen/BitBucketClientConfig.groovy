package org.ods.doc.gen

import feign.Feign
import feign.Logger
import feign.auth.BasicAuthRequestInterceptor
import feign.slf4j.Slf4jLogger
import groovy.util.logging.Slf4j
import okhttp3.OkHttpClient
import org.ods.doc.gen.pdf.builder.repository.BitBucketDocumentTemplatesStoreHttpAPI
import org.springframework.stereotype.Service

@Slf4j
@Service
class BitBucketClientConfig {

    BitBucketDocumentTemplatesStoreHttpAPI getClient(URI baseUrl) {
        def bitbucketUserName = System.getenv("BITBUCKET_USERNAME")
        def bitbucketPassword = System.getenv("BITBUCKET_PASSWORD")
        Feign.Builder builder = Feign.builder()
        if (bitbucketUserName && bitbucketPassword) {
            builder.requestInterceptor(new BasicAuthRequestInterceptor(bitbucketUserName, bitbucketPassword))
        }
        feign.okhttp.OkHttpClient client = new feign.okhttp.OkHttpClient(new OkHttpClient().newBuilder().build())
        return builder.client(client).logger(new Slf4jLogger(BitBucketDocumentTemplatesStoreHttpAPI.class))
                .logLevel(Logger.Level.FULL)
                .target(BitBucketDocumentTemplatesStoreHttpAPI.class, baseUrl.getScheme() + "://" + baseUrl.getAuthority())
    }

}
