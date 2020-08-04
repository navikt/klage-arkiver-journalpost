package no.nav.klage.config

import no.nav.klage.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class OidcDiscoveryClientConfiguration(private val webClientBuilder: WebClient.Builder) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @Value("\${AZURE_APP_WELL_KNOWN_URL}")
    private lateinit var discoveryUrl: String

    @Bean
    fun oidcDiscoveryWebClient(): WebClient {
        return webClientBuilder
                .baseUrl(discoveryUrl)
                .build()
    }
}