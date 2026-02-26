package no.nav.klage.config

import no.nav.klage.util.getLogger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class KlageDittnavAPIClientConfiguration(
    @Qualifier("fastLookupWebClientBuilder") private val fastLookupWebClientBuilder: WebClient.Builder
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @Value($$"${KLAGE-DITTNAV-API_SERVICE_URL}")
    private lateinit var klageDittnavAPIServiceURL: String

    @Bean
    fun klageDittnavAPIWebClient(): WebClient {
        return fastLookupWebClientBuilder
            .baseUrl(klageDittnavAPIServiceURL + "/internal/")
            .build()
    }
}