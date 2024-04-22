package no.nav.klage.clients

import io.github.resilience4j.kotlin.retry.executeFunction
import io.github.resilience4j.retry.Retry
import no.nav.klage.domain.KlageAnkeInput
import no.nav.klage.domain.KlageAnkeType
import no.nav.klage.domain.KlagePDFModel
import no.nav.klage.domain.Vedlegg
import no.nav.klage.getLogger
import no.nav.klage.kodeverk.Enhet
import no.nav.klage.kodeverk.innsendingsytelse.Innsendingsytelse
import no.nav.klage.util.sanitizeText
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.format.DateTimeFormatter
import java.util.*


@Component
class PDFGeneratorClient(
    private val pdfWebClient: WebClient,
    private val retryPdf: Retry
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun generatePDF(klageAnkeInput: KlageAnkeInput): ByteArray {
        return when (klageAnkeInput.klageAnkeType) {
            KlageAnkeType.KLAGE, KlageAnkeType.ANKE -> getKlageAnkePDF(klageAnkeInput)
            KlageAnkeType.KLAGE_ETTERSENDELSE, KlageAnkeType.ANKE_ETTERSENDELSE -> getEttersendelsePDF(klageAnkeInput)
        }
    }

    fun getKlageAnkePDF(klageAnkeInput: KlageAnkeInput): ByteArray {
        logger.debug("Creating PDF for klage/anke.")
        return retryPdf.executeFunction {
            pdfWebClient.post()
                .uri { it.path("/klageanke").build() }
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(klageAnkeInput.toPDFModel())
                .retrieve()
                .bodyToMono<ByteArray>()
                .block() ?: throw RuntimeException("PDF could not be generated")
        }
    }

    fun getEttersendelsePDF(klageAnkeInput: KlageAnkeInput): ByteArray {
        logger.debug("Creating PDF for ettersendelse.")
        return retryPdf.executeFunction {
            pdfWebClient.post()
                .uri { it.path("/ettersendelse").build() }
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(klageAnkeInput.toPDFModel())
                .retrieve()
                .bodyToMono<ByteArray>()
                .block() ?: throw RuntimeException("PDF could not be generated")
        }
    }

    private fun KlageAnkeInput.toPDFModel(): KlagePDFModel {
        val ytelseName = if (innsendingsYtelseId.isNullOrBlank()) {
            ytelse
        } else {
            Innsendingsytelse.of(innsendingsYtelseId).nbName
        }

        return KlagePDFModel(
            type = klageAnkeType.name,
            foedselsnummer = StringBuilder(identifikasjonsnummer).insert(6, " ").toString(),
            fornavn = fornavn,
            mellomnavn = mellomnavn,
            etternavn = etternavn,
            vedtak = vedtak,
            begrunnelse = sanitizeText(begrunnelse),
            saksnummer = sanitizeText(getSaksnummerString(userSaksnummer, internalSaksnummer)),
            oversiktVedlegg = getOversiktVedlegg(vedlegg),
            dato = dato.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
            ytelse = ytelseName.replaceFirstChar { it.lowercase(Locale.getDefault()) },
            userChoices = userChoices,
            enhetsnavn = Enhet.entries.find { it.navn == enhetsnummer }?.beskrivelse
        )
    }

    private fun getOversiktVedlegg(vedlegg: List<Vedlegg>): String {
        return if (vedlegg.isEmpty()) {
            "Ingen vedlegg."
        } else {
            vedlegg.joinToString { it.tittel }
        }
    }

    private fun getSaksnummerString(userSaksnummer: String? = null, internalSaksnummer: String? = null): String {
        return when {
            userSaksnummer != null -> {
                "$userSaksnummer - Oppgitt av bruker"
            }

            internalSaksnummer != null -> {
                "$internalSaksnummer - Hentet fra internt system"
            }

            else -> "Ikke angitt"
        }
    }
}

