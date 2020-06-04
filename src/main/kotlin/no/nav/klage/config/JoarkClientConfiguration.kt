package no.nav.klage.config

import no.nav.klage.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class JoarkClientConfiguration() {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @Value("\${JOARK_SERVICE_URL}")
    private lateinit var joarkServiceURL: String

    @Value("\${JOURNALPOST_APIKEY}")
    private lateinit var apiKey: String

    @Bean
    fun joarkWebClient(): WebClient {
        return WebClient
            .builder()
            .defaultHeader("x-nav-apiKey", apiKey)
            .baseUrl(joarkServiceURL)
            .build()
    }
}