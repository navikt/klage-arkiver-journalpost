package no.nav.klage.clients

import no.nav.klage.domain.KlageAnkeInput
import no.nav.klage.domain.KlageAnkeType
import no.nav.klage.domain.KlagePDFModel
import no.nav.klage.domain.Vedlegg
import no.nav.klage.kodeverk.innsendingsytelse.Innsendingsytelse
import no.nav.klage.util.getLogger
import no.nav.klage.util.sanitizeText
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.MediaType
import org.springframework.resilience.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import java.io.File
import java.nio.file.Files
import java.time.format.DateTimeFormatter
import java.util.*


@Component
class PDFGeneratorClient(
    private val pdfWebClient: WebClient,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @Retryable
    fun generatePDF(klageAnkeInput: KlageAnkeInput): File {
        return when (klageAnkeInput.klageAnkeType) {
            KlageAnkeType.KLAGE, KlageAnkeType.ANKE -> getKlageAnkePDF(klageAnkeInput)
            KlageAnkeType.KLAGE_ETTERSENDELSE, KlageAnkeType.ANKE_ETTERSENDELSE -> getEttersendelsePDF(klageAnkeInput)
        }
    }

    private fun getKlageAnkePDF(klageAnkeInput: KlageAnkeInput): File {
        logger.debug("Creating PDF for klage/anke.")

        val dataBufferFlux = pdfWebClient.post()
            .uri { it.path("/klageanke").build() }
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(klageAnkeInput.toPDFModel())
            .retrieve()
            .bodyToFlux<DataBuffer>()

        val tempFile = Files.createTempFile(null, null)

        DataBufferUtils.write(dataBufferFlux, tempFile).block()
        return tempFile.toFile()
    }

    private fun getEttersendelsePDF(klageAnkeInput: KlageAnkeInput): File {
        logger.debug("Creating PDF for ettersendelse.")
        val dataBufferFlux = pdfWebClient.post()
            .uri { it.path("/ettersendelse").build() }
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(klageAnkeInput.toPDFModel())
            .retrieve()
            .bodyToFlux<DataBuffer>()

        val tempFile = Files.createTempFile(null, null)

        DataBufferUtils.write(dataBufferFlux, tempFile).block()
        return tempFile.toFile()
    }

    private fun KlageAnkeInput.toPDFModel(): KlagePDFModel {
        val ytelseName = Innsendingsytelse.of(innsendingsYtelseId).nbName

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
            ytelse = formatYtelseName(ytelseName),
            ettersendelseTilKa = ettersendelseTilKa ?: false,
        )
    }

    private fun formatYtelseName(ytelseName: String): String {
        return if (ytelseName[1].isUpperCase()) {
            ytelseName
        } else ytelseName.replaceFirstChar { it.lowercase(Locale.getDefault()) }
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

