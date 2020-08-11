package no.nav.klage.config

import no.nav.klage.clients.OidcDiscoveryClient
import no.nav.klage.getLogger
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class AzureADClientConfiguration(
        private val webClientBuilder: WebClient.Builder,
        private val oidcDiscoveryClient: OidcDiscoveryClient
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @Bean
    fun azureADWebClient(): WebClient {
        return webClientBuilder
                .build()
    }
}