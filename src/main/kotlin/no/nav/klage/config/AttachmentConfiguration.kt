package no.nav.klage.config

import no.nav.klage.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class AttachmentConfiguration {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @Value("\${VEDLEGG_SERVICE_URL}")
    private lateinit var attachmentServiceURL: String

    @Bean
    fun attachmentWebClient(): WebClient {
        return WebClient
            .builder()
            .baseUrl(attachmentServiceURL)
            .build()
    }
}