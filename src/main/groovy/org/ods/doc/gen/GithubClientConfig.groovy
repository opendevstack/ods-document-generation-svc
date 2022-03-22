package org.ods.doc.gen

import feign.Feign
import feign.Logger
import feign.slf4j.Slf4jLogger
import groovy.util.logging.Slf4j
import okhttp3.OkHttpClient
import org.ods.doc.gen.external.modules.git.GitHubRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Slf4j
@Service
class GithubClientConfig {

    String url
    String[] httpProxyHost

    GithubClientConfig(@Value('${github.url}') String url){
        log.info("GithubClientConfig - url:[${url}]")

        this.url = url
        httpProxyHost = System.getenv('HTTP_PROXY')?.trim()?.replace('http://', '')?.split(':')
    }

    GitHubRepository getClient() {
        feign.okhttp.OkHttpClient client
        if (httpProxyHost) {
            log.info ("Proxy setup: ${httpProxyHost}")
            int httpProxyPort = httpProxyHost.size() == 2 ? Integer.parseInt(httpProxyHost[1]) : 80
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(httpProxyHost[0], httpProxyPort))
            client = new feign.okhttp.OkHttpClient(new OkHttpClient().newBuilder().proxy(proxy).build())
        } else {
            client = new feign.okhttp.OkHttpClient(new OkHttpClient().newBuilder().build())
        }
        URI baseUrl = URI.create(url)
        return Feign.builder().client(client).logger(new Slf4jLogger(GitHubRepository.class))
                .logLevel(Logger.Level.BASIC)
                .target(GitHubRepository.class, baseUrl.getScheme() + "://" + baseUrl.getAuthority())
    }

}
