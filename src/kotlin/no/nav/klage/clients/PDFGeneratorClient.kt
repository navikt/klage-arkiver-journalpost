package no.nav.klage.clients

import no.nav.klage.domain.Klage
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono


@Component
class PDFGeneratorClient(private val pdfWebClient: WebClient) {

    fun getFilledOutPDF(klage: Klage): ByteArray {
        return pdfWebClient.post()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(klage)
            .retrieve()
            .bodyToMono<ByteArray>()
            .block() ?: throw RuntimeException("PDF could not be generated")
    }
}