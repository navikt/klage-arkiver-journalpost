package no.nav.klage.config

import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GcsConfiguration {
    @Bean
    fun gcsStorage(): Storage = StorageOptions.getDefaultInstance().service
}