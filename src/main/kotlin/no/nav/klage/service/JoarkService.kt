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
    private val joarkClient: JoarkClient,
    private val pdfService: PdfService
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()

        private const val KLAGE_ID_KEY = "klage_id"
        private const val ANKE_ID_KEY = "anke_id"
        private const val KLAGE_YTELSE_KEY = "klage_ytelse"
        private const val KLAGE_TITTEL = "Klage"
        private const val ANKE_TITTEL = "Anke"
        private const val BREVKODE_KLAGESKJEMA_KLAGE = "NAV 90-00.08 K"
        private const val BREVKODE_KLAGESKJEMA_ANKE = "NAV 90-00.08 A"
        private const val PDF_CODE = "PDF"
        private const val PDFA_CODE = "PDFA"

    }

    fun createJournalpostInJoark(klageAnkeInput: KlageAnkeInput): String {
        logger.debug("Creating journalpost.")

        val journalpost = getJournalpost(klageAnkeInput)

        val journalpostAsJSONForLogging = getJournalpostAsJSONForLogging(journalpost)
        secureLogger.debug("Journalpost as JSON (what we post to dokarkiv/Joark): {}", journalpostAsJSONForLogging)

        return joarkClient.postJournalpost(journalpost, klageAnkeInput.id)
    }

    private fun getJournalpost(klageAnkeInput: KlageAnkeInput): Journalpost {
        return Journalpost(
            tema = klageAnkeInput.tema,
            behandlingstema = klageAnkeInput.getBehandlingstema(),
            avsenderMottaker = AvsenderMottaker(
                id = klageAnkeInput.identifikasjonsnummer,
                navn = getFullName(klageAnkeInput),
            ),
            sak = getSak(klageAnkeInput),
            tittel = when (klageAnkeInput.klageAnkeType) {
                KlageAnkeType.KLAGE -> KLAGE_TITTEL
                KlageAnkeType.ANKE -> ANKE_TITTEL
            },
            bruker = Bruker(
                id = klageAnkeInput.identifikasjonsnummer,
            ),
            dokumenter = getDokumenter(klageAnkeInput),
            tilleggsopplysninger = when (klageAnkeInput.klageAnkeType) {
                KlageAnkeType.KLAGE -> listOf(
                    Tilleggsopplysning(nokkel = KLAGE_ID_KEY, verdi = klageAnkeInput.id),
                    Tilleggsopplysning(nokkel = KLAGE_YTELSE_KEY, verdi = klageAnkeInput.ytelse)
                )
                KlageAnkeType.ANKE -> listOf(
                    Tilleggsopplysning(nokkel = ANKE_ID_KEY, verdi = klageAnkeInput.internalSaksnummer.toString()),
                    Tilleggsopplysning(nokkel = KLAGE_YTELSE_KEY, verdi = klageAnkeInput.ytelse)
                )
            },
            eksternReferanseId = "${klageAnkeInput.klageAnkeType.name}_${klageAnkeInput.id}",
        )
    }

    private fun getFullName(klageAnkeInput: KlageAnkeInput): String {
        return if (klageAnkeInput.mellomnavn.isBlank()) {
            "${klageAnkeInput.fornavn} ${klageAnkeInput.etternavn}"
        } else {
            "${klageAnkeInput.fornavn} ${klageAnkeInput.mellomnavn} ${klageAnkeInput.etternavn}"
        }
    }

    private fun getSak(klageAnkeInput: KlageAnkeInput): Sak? =
        if (klageAnkeInput.tema == "FOR" && klageAnkeInput.internalSaksnummer?.toIntOrNull() != null) {
            Sak(
                sakstype = Sakstype.FAGSAK,
                fagsaksystem = FagsaksSystem.FS36,
                fagsakid = klageAnkeInput.internalSaksnummer
            )
        } else {
            null
        }

    private fun getDokumenter(klageAnkeInput: KlageAnkeInput): List<Dokument> {
        val hovedDokument = Dokument(
            tittel = when (klageAnkeInput.klageAnkeType) {
                KlageAnkeType.KLAGE -> KLAGE_TITTEL
                KlageAnkeType.ANKE -> ANKE_TITTEL
            },
            brevkode = when (klageAnkeInput.klageAnkeType) {
                KlageAnkeType.KLAGE -> BREVKODE_KLAGESKJEMA_KLAGE
                KlageAnkeType.ANKE -> BREVKODE_KLAGESKJEMA_ANKE
            },
            //Don't perform pdfa-check for now. Issues with compatibility with Vera and Spring Boot 3.
            dokumentVarianter = getDokumentVariant(klageAnkeInput.fileContentAsBytes, performPdfaCheck = false)
        )
        val documents = mutableListOf(hovedDokument)

        klageAnkeInput.vedlegg.forEach {
            //Attachments will always be PDF as of now.
            secureLogger.debug("Adding attachment with title ${it.tittel} and GCS reference ${it.ref} to journalpost")
            val doc = Dokument(
                tittel = it.tittel,
                dokumentVarianter = getDokumentVariant(bytes = it.fileContentAsBytes, performPdfaCheck = false)
            )
            documents.add(doc)
        }
        return documents
    }

    private fun getDokumentVariant(bytes: ByteArray?, performPdfaCheck: Boolean = true): List<DokumentVariant> {
        return if (bytes != null) {
            val dokumentVariant = DokumentVariant(
                filtype = if (performPdfaCheck && pdfService.pdfByteArrayIsPdfa(bytes)) PDFA_CODE else PDF_CODE,
                variantformat = "ARKIV",
                fysiskDokument = Base64.getEncoder().encodeToString(bytes)
            )
            listOf(dokumentVariant)
        } else emptyList()
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
}