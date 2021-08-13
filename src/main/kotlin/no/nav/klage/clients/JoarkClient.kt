package no.nav.klage.clients

import brave.Tracer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.klage.domain.*
import no.nav.klage.getLogger
import no.nav.klage.getSecureLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.util.*


@Component
class JoarkClient(
    private val joarkWebClient: WebClient,
    private val stsClient: StsClient,
    private val tracer: Tracer
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()

        private const val KLAGE_ID_KEY = "klage_id"
        private const val KLAGE_TITTEL = "Klage/Anke"
        private const val BREVKODE_KLAGESKJEMA = "NAV 90-00.08"
        private const val BEHANDLINGSTEMA_LONNSKOMPENSASJON = "ab0438"
        private const val BEHANDLINGSTEMA_TILBAKEBETALING_FORSKUDD_PAA_DAGPENGER = "ab0451"
        private const val BEHANDLINGSTEMA_FERIEPENGER_AV_DAGPENGER = "ab0452"

    }

    @Value("\${DRY_RUN}")
    private lateinit var dryRun: String

    fun createJournalpost(klageAnkeInput: KlageAnkeInput): String {
        logger.debug("Creating journalpost.")

        val journalpost = getJournalpost(klageAnkeInput)

        val journalpostAsJSONForLogging = getJournalpostAsJSONForLogging(journalpost)
        secureLogger.debug("Journalpost as JSON (what we post to dokarkiv/Joark): {}", journalpostAsJSONForLogging)

        return if (dryRun.toBoolean()) {
            logger.debug("Dry run activated. Not sending journalpost to Joark.")
            "dryRun, no journalpostId"
        } else {
            logger.debug("Posting journalpost to Joark.")
            val journalpostResponse = joarkWebClient.post()
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${stsClient.oidcToken()}")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(journalpost)
                .retrieve()
                .bodyToMono(JournalpostResponse::class.java)
                .block() ?: throw RuntimeException("Journalpost could not be created for klage with id ${klageAnkeInput.id}.")

            logger.debug("Journalpost successfully created in Joark with id {}.", journalpostResponse.journalpostId)

            journalpostResponse.journalpostId
        }
    }

    private fun getJournalpost(klageAnkeInput: KlageAnkeInput): Journalpost =
        Journalpost(
            tema = klageAnkeInput.tema,
            behandlingstema = getBehandlingstema(klageAnkeInput),
            avsenderMottaker = AvsenderMottaker(
                id = klageAnkeInput.fullmektigFnr ?: klageAnkeInput.identifikasjonsnummer,
                navn = klageAnkeInput.fullmektigNavn ?: "${klageAnkeInput.fornavn} ${klageAnkeInput.mellomnavn} ${klageAnkeInput.etternavn}"
            ),
            sak = getSak(klageAnkeInput),
            tittel = KLAGE_TITTEL,
            bruker = Bruker(
                id = klageAnkeInput.identifikasjonsnummer,
            ),            
            dokumenter = getDokumenter(klageAnkeInput),
            tilleggsopplysninger = listOf(Tilleggsopplysning(nokkel = KLAGE_ID_KEY, verdi = klageAnkeInput.id.toString()))
        )

    private fun getSak(klageAnkeInput: KlageAnkeInput): Sak? =
        if (klageAnkeInput.tema == "FOR" && klageAnkeInput.internalSaksnummer?.toIntOrNull() != null) {
            Sak(sakstype = Sakstype.FAGSAK, fagsaksystem = FagsaksSystem.FS36, fagsakid = klageAnkeInput.internalSaksnummer)
        } else {
            null
        }

    private fun getDokumenter(klageAnkeInput: KlageAnkeInput): List<Dokument> {
        val hovedDokument = Dokument(
            tittel = KLAGE_TITTEL,
            brevkode = BREVKODE_KLAGESKJEMA,
            dokumentVarianter = getDokumentVariant(klageAnkeInput.fileContentAsBytes, "PDFA")
        )
        val documents = mutableListOf(hovedDokument)

        klageAnkeInput.vedlegg.forEach {
            //Attachments will always be PDF as of now.
            val doc = Dokument(
                tittel = it.tittel,
                dokumentVarianter = getDokumentVariant(it.fileContentAsBytes, "PDF")
            )
            documents.add(doc)
        }
        return documents
    }

    private fun getDokumentVariant(bytes: ByteArray?, fileType: String): List<DokumentVariant> {
        val dokumentVariant = DokumentVariant(
            filtype = fileType,
            variantformat = "ARKIV",
            fysiskDokument = Base64.getEncoder().encodeToString(bytes)
        )
        return listOf(dokumentVariant)
    }

    private fun getJournalpostAsJSONForLogging(journalpost: Journalpost): String {
        val dokumenterWithoutFileData = journalpost.dokumenter.map { dokument ->
            dokument.copy(dokumentVarianter = dokument.dokumentVarianter.map { variant ->
                variant.copy(fysiskDokument = "base64 data removed for logging purposes")
            })
        }
        val journalpostCopyWithoutFileData = journalpost.copy(dokumenter = dokumenterWithoutFileData)
        return jacksonObjectMapper().writeValueAsString(journalpostCopyWithoutFileData)
    }

    private fun getBehandlingstema(klageAnkeInput: KlageAnkeInput): String? {
        return when {
            klageAnkeInput.isLoennskompensasjon() -> BEHANDLINGSTEMA_LONNSKOMPENSASJON
            klageAnkeInput.isTilbakebetalingAvForskuddPaaDagpenger() -> BEHANDLINGSTEMA_TILBAKEBETALING_FORSKUDD_PAA_DAGPENGER
            klageAnkeInput.isFeriepengerAvDagpenger() -> BEHANDLINGSTEMA_FERIEPENGER_AV_DAGPENGER
            else -> null
        }
    }
}