package org.ods.doc.gen

import groovy.util.logging.Slf4j
import kong.unirest.Unirest
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct

@Slf4j
@Component
class StartupApplicationListener implements ApplicationListener<ContextRefreshedEvent> {

    @Override void onApplicationEvent(ContextRefreshedEvent event) {
        log.trace(event.toString())
    }

    @PostConstruct
    void init() {
        Unirest.config().reset().socketTimeout(6000000).connectTimeout(600000).verifySsl(false)
    }

}