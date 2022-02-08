package org.ods.doc.gen


import feign.auth.BasicAuthRequestInterceptor
import org.springframework.context.annotation.Bean

class GithubClientConfig {

  /*  @Bean
    RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("Accept", "aapplication/octet-stream");
        };
    }
*/

    @Bean
    BasicAuthRequestInterceptor basicAuthRequestInterceptor() {
        def bitbucketUserName = System.getenv("BITBUCKET_USERNAME")
        def bitbucketPassword = System.getenv("BITBUCKET_PASSWORD")
        return new BasicAuthRequestInterceptor(bitbucketUserName, bitbucketPassword)
    }
}
