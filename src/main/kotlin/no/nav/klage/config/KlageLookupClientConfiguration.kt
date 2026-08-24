package no.nav.klage.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class KlageLookupClientConfiguration(
    @Qualifier("fastLookupWebClientBuilder")  private val fastLookupWebClientBuilder: WebClient.Builder
) {

    @Value($$"${KLAGE_LOOKUP_BASE_URL}")
    private lateinit var klageLookupUrl: String

    @Bean
    fun klageLookupWebClient(): WebClient {
        return fastLookupWebClientBuilder
            .baseUrl(klageLookupUrl)
            .build()
    }
}