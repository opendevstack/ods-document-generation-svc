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

    final String username
    private final String password
    String url

    BitBucketClientConfig(@Value('${bitbucket.username}') String username,
                          @Value('${bitbucket.password}') String password,
                          @Value('${bitbucket.url}') String url){
        log.info("BitBucketClientConfig - url:[${url}], username:[${username}]")

        this.password = password
        this.username = username
        this.url = url
    }

    BitBucketRepository getClient() {
        URI baseUrl = new URIBuilder(url).build()
        Feign.Builder builder = Feign.builder()
        builder.requestInterceptor(new BasicAuthRequestInterceptor(username, password))
        feign.okhttp.OkHttpClient client = new feign.okhttp.OkHttpClient(new OkHttpClient().newBuilder().build())
        return builder.client(client).logger(new Slf4jLogger(BitBucketRepository.class))
                .logLevel(Logger.Level.BASIC)
                .target(BitBucketRepository.class, baseUrl.getScheme() + "://" + baseUrl.getAuthority())
    }

}
