package no.nav.klage.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class PDFGeneratorConfiguration {

    @Value("\${PDF_SERVICE_URL}")
    private lateinit var pdfServiceURL: String

    @Bean
    fun pdfWebClient(): WebClient {
        return WebClient
            .builder()
            .baseUrl(pdfServiceURL)
            .build()
    }
}