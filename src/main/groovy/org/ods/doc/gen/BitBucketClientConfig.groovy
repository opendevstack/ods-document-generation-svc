package org.ods.doc.gen

import feign.Feign
import feign.Logger
import feign.auth.BasicAuthRequestInterceptor
import feign.slf4j.Slf4jLogger
import groovy.util.logging.Slf4j
import okhttp3.OkHttpClient
import org.apache.http.client.utils.URIBuilder
import org.ods.doc.gen.external.modules.git.BitBucketRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Slf4j
@Service
class BitBucketClientConfig {

    private final String username
    private final String password

    BitBucketClientConfig(@Value('${bitbucket.username}') String username,
                          @Value('${bitbucket.password}') String password){
        this.password = password
        this.username = username
    }

    BitBucketRepository getClient() {
        URI baseUrl = new URIBuilder(System.getenv("BITBUCKET_URL") as String).build()
        Feign.Builder builder = Feign.builder()
        builder.requestInterceptor(new BasicAuthRequestInterceptor(username, password))
        feign.okhttp.OkHttpClient client = new feign.okhttp.OkHttpClient(new OkHttpClient().newBuilder().build())
        return builder.client(client).logger(new Slf4jLogger(BitBucketRepository.class))
                .logLevel(Logger.Level.FULL)
                .target(BitBucketRepository.class, baseUrl.getScheme() + "://" + baseUrl.getAuthority())
    }

}
