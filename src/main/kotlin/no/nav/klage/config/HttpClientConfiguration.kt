package no.nav.klage.config

import no.nav.klage.clients.StsClient
import no.nav.klage.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class HttpClientConfiguration(private val stsClient: StsClient) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @Value("\${PDF_SERVICE_URL}")
    private lateinit var pdfServiceURL: String

    @Value("\${JOARK_SERVICE_URL}")
    private lateinit var joarkServiceURL: String

    @Bean
    fun pdfWebClient(): WebClient {
        return WebClient
            .builder()
            .baseUrl(pdfServiceURL)
            .build()
    }

    @Bean
    fun joarkWebClient(): WebClient {
        return WebClient
            .builder()
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer ${stsClient.oidcToken()}")
            .baseUrl(joarkServiceURL)
            .build()
    }
}