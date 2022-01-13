package org.ods.doc.gen


import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.PropertySource

@TestConfiguration
@PropertySource("classpath:application.properties")
@ComponentScan("org.ods")
class AppConfig {

}