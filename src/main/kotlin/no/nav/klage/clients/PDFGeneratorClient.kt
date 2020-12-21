package no.nav.klage.clients

import io.github.resilience4j.kotlin.retry.executeFunction
import io.github.resilience4j.retry.Retry
import no.nav.klage.domain.Klage
import no.nav.klage.domain.KlagePDFModel
import no.nav.klage.domain.Vedlegg
import no.nav.klage.getLogger
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.format.DateTimeFormatter


@Component
class PDFGeneratorClient(
    private val pdfWebClient: WebClient,
    private val retryPdf: Retry
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun getFilledOutPDF(klage: Klage): ByteArray {
        logger.debug("Creating PDF from klage.")
        return retryPdf.executeFunction { pdfWebClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(klage.toPDFModel())
                .retrieve()
                .bodyToMono<ByteArray>()
                .block() ?: throw RuntimeException("PDF could not be generated")
        }
    }

    private fun Klage.toPDFModel() = KlagePDFModel(
        foedselsnummer = StringBuilder(identifikasjonsnummer).insert(6, " ").toString(),
        fornavn = fornavn,
        mellomnavn = mellomnavn,
        etternavn = etternavn,
        adresse = adresse,
        telefonnummer = telefon,
        vedtak = vedtak,
        begrunnelse = begrunnelse,
        saksnummer = getSaksnummerString(userSaksnummer, internalSaksnummer, saksnummer),
        oversiktVedlegg = getOversiktVedlegg(vedlegg),
        dato = dato.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
        ytelse = ytelse,
        userChoices = userChoices
    )

    private fun getOversiktVedlegg(vedlegg: List<Vedlegg>): String {
        return if (vedlegg.isEmpty()) {
            "Ingen vedlegg."
        } else {
            vedlegg.joinToString { it.tittel }
        }
    }

    private fun getSaksnummerString(userSaksnummer: String? = null, internalSaksnummer: String? = null, saksnummer: String? = null): String {
        return when {
            userSaksnummer != null -> {
                "$userSaksnummer - Oppgitt av bruker"
            }
            internalSaksnummer != null -> {
                "$internalSaksnummer - Hentet fra internt system"
            }
            else -> saksnummer ?: "Ikke angitt"
        }
    }
}

