package org.ods.doc.gen

import feign.Feign
import feign.Logger
import feign.slf4j.Slf4jLogger
import groovy.util.logging.Slf4j
import okhttp3.OkHttpClient
import org.ods.doc.gen.pdf.builder.repository.GithubDocumentTemplatesStoreHttpAPI
import org.springframework.stereotype.Service

@Slf4j
@Service
class GithubClientConfig {

    GithubDocumentTemplatesStoreHttpAPI getClient(URI baseUrl) {
        String[] httpProxyHost = System.getenv('HTTP_PROXY')?.trim()?.replace('http://','')?.split(':')
        log.info ("Proxy setup: ${httpProxyHost ?: 'not found' }")

        feign.okhttp.OkHttpClient client
        if (httpProxyHost && !System.getenv("GITHUB_HOST")) {
            int httpProxyPort = httpProxyHost.size() == 2 ? Integer.parseInt(httpProxyHost[1]) : 80
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(httpProxyHost[0], httpProxyPort))
            client = new feign.okhttp.OkHttpClient(new OkHttpClient().newBuilder().proxy(proxy).build())
        } else {
            client = new feign.okhttp.OkHttpClient(new OkHttpClient().newBuilder().build())
        }

        return Feign.builder().client(client).logger(new Slf4jLogger(GithubDocumentTemplatesStoreHttpAPI.class))
                .logLevel(Logger.Level.FULL)
                .target(GithubDocumentTemplatesStoreHttpAPI.class, baseUrl.getScheme() + "://" + baseUrl.getAuthority())
    }

}
