package org.ods.doc.gen

import com.github.benmanes.caffeine.cache.Caffeine
import org.apache.commons.io.FileUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import java.nio.file.Paths
import java.time.Clock
import java.time.Duration

@EnableCaching
@EnableFeignClients
@Configuration
class AppConfiguration {

    private static final String TEMPLATES = "templates"
    private static final String TEMPORAL_FOLDER = "temporalFolder"
    private static final String PROJECT_DATA = "projectData"

    @Bean
    Clock aClockBeanToMockInTesting() {
        return Clock.systemDefaultZone()
    }

    @Bean
    CaffeineCache caffeineTemplatesFolder(@Value('${cache.documents.basePath}') String basePath) {
        FileUtils.deleteDirectory(Paths.get(basePath).toFile())
        return new CaffeineCache(
                TEMPLATES,
                Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofDays(1))
                .removalListener({ version, graph, cause ->
                    FileUtils.deleteDirectory(Paths.get(basePath, version as String).toFile())
                }).build()
        )
    }

    @Bean
    CaffeineCache caffeineTemporalFolder() {
        return new CaffeineCache(
                TEMPORAL_FOLDER,
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofDays(1))
                        .removalListener({ id, graph, cause ->
                            FileUtils.deleteDirectory(Paths.get(id as String).toFile())
                        }).build()
        )
    }

    @Bean
    CaffeineCache caffeineProjectDataConfig(@Value('${cache.projectData.expiration.minutes}') Long expirationMinutes) {
        return new CaffeineCache(
                PROJECT_DATA,
                Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(expirationMinutes)).build()
        )
    }

}
