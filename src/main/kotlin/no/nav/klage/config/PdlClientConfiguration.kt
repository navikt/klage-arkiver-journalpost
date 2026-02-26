package no.nav.klage.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

@Configuration
class PdlClientConfiguration(
    @Qualifier("fastLookupWebClientBuilder") private val fastLookupWebClientBuilder: WebClient.Builder
) {

    @Value($$"${PDL_BASE_URL}")
    private lateinit var pdlUrl: String

    @Value($$"${SERVICE_USER_USERNAME}")
    private lateinit var username: String

    @Bean
    fun pdlWebClient(): WebClient {
        return fastLookupWebClientBuilder
            .baseUrl(pdlUrl)
            .clientConnector(ReactorClientHttpConnector(HttpClient.newConnection()))
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("Nav-Consumer-Id", username)
            .defaultHeader("TEMA", "KLA")
            //Fra behandlingskatalogen
            .defaultHeader("behandlingsnummer", "B392")
            .build()
    }
}