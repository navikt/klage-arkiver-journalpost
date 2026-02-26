package no.nav.klage.config

import no.nav.klage.util.getLogger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class PDFClientConfiguration(
    @Qualifier("standardWebClientBuilder") private val standardWebClientBuilder: WebClient.Builder
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @Value($$"${PDF_SERVICE_URL}")
    private lateinit var pdfServiceURL: String

    @Bean
    fun pdfWebClient(): WebClient {
        return standardWebClientBuilder
            .baseUrl(pdfServiceURL)
            .build()
    }

}