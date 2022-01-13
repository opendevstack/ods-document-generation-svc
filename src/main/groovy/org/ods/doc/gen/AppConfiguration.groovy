package org.ods.doc.gen

import com.github.benmanes.caffeine.cache.Caffeine
import org.apache.commons.io.FileUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

import java.nio.file.Paths
import java.time.Duration

@EnableCaching
@Configuration
class AppConfiguration {
    @Bean
    Caffeine caffeineConfig(@Value('${documents.cache.basePath}') String basePath) {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofDays(1))
                .removalListener({ version, graph, cause ->
                    FileUtils.deleteDirectory(Paths.get(basePath, version as String).toFile())
                })
    }

    @Bean
    CacheManager cacheManager(Caffeine caffeine) {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(caffeine);
        return caffeineCacheManager;
    }
}
