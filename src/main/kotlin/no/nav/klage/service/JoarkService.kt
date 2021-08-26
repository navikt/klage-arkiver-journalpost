package no.nav.klage.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.klage.clients.JoarkClient
import no.nav.klage.domain.*
import no.nav.klage.getLogger
import no.nav.klage.getSecureLogger
import org.springframework.stereotype.Service
import java.util.*

@Service
class JoarkService(
    private val joarkClient: JoarkClient
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()

        private const val KLAGE_ID_KEY = "klage_id"
        private const val ANKE_ID_KEY = "anke_id"
        private const val KLAGE_TITTEL = "Klage"
        private const val ANKE_TITTEL = "Anke"
        private const val BREVKODE_KLAGESKJEMA = "NAV 90-00.08"
        private const val BREVKODE_KLAGESKJEMA_ANKE = "NAV 90-00.08 A"
        private const val BEHANDLINGSTEMA_LONNSKOMPENSASJON = "ab0438"
        private const val BEHANDLINGSTEMA_TILBAKEBETALING_FORSKUDD_PAA_DAGPENGER = "ab0451"
        private const val BEHANDLINGSTEMA_FERIEPENGER_AV_DAGPENGER = "ab0452"

    }

    fun createJournalpostInJoark(klageAnkeInput: KlageAnkeInput): String {
        logger.debug("Creating journalpost.")

        val journalpost = getJournalpost(klageAnkeInput)

        val journalpostAsJSONForLogging = getJournalpostAsJSONForLogging(journalpost)
        secureLogger.debug("Journalpost as JSON (what we post to dokarkiv/Joark): {}", journalpostAsJSONForLogging)

        return joarkClient.postJournalpost(journalpost, klageAnkeInput.id.toString())

    }

    private fun getJournalpost(klageAnkeInput: KlageAnkeInput): Journalpost {
        return Journalpost(
            tema = klageAnkeInput.tema,
            behandlingstema = if (klageAnkeInput.isKlage()) getBehandlingstema(klageAnkeInput) else null,
            avsenderMottaker = AvsenderMottaker(
                id = klageAnkeInput.fullmektigFnr ?: klageAnkeInput.identifikasjonsnummer,
                navn = klageAnkeInput.fullmektigNavn
                    ?: "${klageAnkeInput.fornavn} ${klageAnkeInput.mellomnavn} ${klageAnkeInput.etternavn}"
            ),
            sak = getSak(klageAnkeInput),
            tittel = if (klageAnkeInput.isKlage()) KLAGE_TITTEL else ANKE_TITTEL,
            bruker = Bruker(
                id = klageAnkeInput.identifikasjonsnummer,
            ),
            dokumenter = getDokumenter(klageAnkeInput),
            tilleggsopplysninger = if (klageAnkeInput.isKlage()) {
                listOf(Tilleggsopplysning(nokkel = KLAGE_ID_KEY, verdi = klageAnkeInput.id.toString()))
            } else {
                listOf(Tilleggsopplysning(nokkel = ANKE_ID_KEY, verdi = klageAnkeInput.internalSaksnummer.toString()))
            }
        )
    }

    private fun getSak(klageAnkeInput: KlageAnkeInput): Sak? =
        if (klageAnkeInput.tema == "FOR" && klageAnkeInput.internalSaksnummer?.toIntOrNull() != null) {
            Sak(sakstype = Sakstype.FAGSAK, fagsaksystem = FagsaksSystem.FS36, fagsakid = klageAnkeInput.internalSaksnummer)
        } else {
            null
        }

    private fun getDokumenter(klageAnkeInput: KlageAnkeInput): List<Dokument> {
        val hovedDokument = Dokument(
            tittel = if (klageAnkeInput.isKlage()) KLAGE_TITTEL else ANKE_TITTEL,
            brevkode = if (klageAnkeInput.isKlage()) BREVKODE_KLAGESKJEMA else BREVKODE_KLAGESKJEMA_ANKE,
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