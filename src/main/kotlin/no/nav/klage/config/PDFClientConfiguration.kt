package no.nav.klage.config

import no.nav.klage.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class PDFClientConfiguration() {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

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