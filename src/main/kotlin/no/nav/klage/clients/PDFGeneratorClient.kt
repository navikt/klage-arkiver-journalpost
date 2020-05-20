package no.nav.klage.clients

import no.nav.klage.domain.Klage
import no.nav.klage.domain.KlagePDFModel
import no.nav.klage.getLogger
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.format.DateTimeFormatter


@Component
class PDFGeneratorClient(private val pdfWebClient: WebClient) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)

        private const val USER_SIGNATURE = "Sendt inn digitalt via nav.no"
    }

    fun getFilledOutPDF(klage: Klage): ByteArray {
        logger.debug("Creating PDF from klage.")
        return pdfWebClient.post()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(klage.toPDFModel())
            .retrieve()
            .bodyToMono<ByteArray>()
            .block() ?: throw RuntimeException("PDF could not be generated")
    }

    private fun Klage.toPDFModel() = KlagePDFModel(
        foedselsnummer = foedselsnummer,
        fornavn = "TODO fornavn",
        etternavn = "TODO Kun etternavn $navn",
        adresse = adresse,
        postnummer = "TODO postnummer",
        poststed = "TODO poststed",
        telefonnummer = telefon,
        NAVenhet = navenhet,
        vedtaksdato = vedtaksdato.format(DateTimeFormatter.ISO_LOCAL_DATE),
        shortWhyShouldChange = kortRedegjoerelse,
        longWhyShouldChange = begrunnelse,
        NAVReference = navReferanse,
        attachments = oversiktVedlegg,
        place = sted,
        date = dato.format(DateTimeFormatter.ISO_LOCAL_DATE),
        userSignature = USER_SIGNATURE,
        NAVSignature = "TODO"
    )
}

