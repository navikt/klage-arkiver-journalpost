package no.nav.klage.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.klage.clients.FileClient
import no.nav.klage.clients.JoarkClient
import no.nav.klage.clients.PDFGeneratorClient
import no.nav.klage.clients.pdl.PdlClient
import no.nav.klage.clients.pdl.PdlPerson
import no.nav.klage.domain.*
import no.nav.klage.kodeverk.Tema
import no.nav.klage.kodeverk.innsendingsytelse.Innsendingsytelse
import no.nav.klage.kodeverk.innsendingsytelse.innsendingsytelseToAnkeEnhet
import no.nav.klage.kodeverk.innsendingsytelse.innsendingsytelseToTema
import no.nav.klage.util.getLogger
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.*

@Service
class JoarkService(
    private val joarkClient: JoarkClient,
    private val pdlClient: PdlClient,
    private val pdfGenerator: PDFGeneratorClient,
    private val fileClient: FileClient,
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)

        private const val KLAGE_ANKE_ID_KEY = "klage_anke_id"
        private const val KLAGE_ANKE_YTELSE_KEY = "klage_anke_ytelse"
        private const val KLAGE_TITTEL = "Klage"
        private const val KLAGE_ETTERSENDELSE_TITTEL = "Ettersendelse til klage"
        private const val ANKE_TITTEL = "Anke"
        private const val ANKE_ETTERSENDELSE_TITTEL = "Ettersendelse til anke"
        private const val BREVKODE_KLAGESKJEMA_KLAGE = "NAV 90-00.08 K"
        private const val BREVKODE_KLAGESKJEMA_KLAGE_ETTERSENDELSE = "NAVe 90-00.08 K"
        private const val BREVKODE_KLAGESKJEMA_ANKE = "NAV 90-00.08 A"
        private const val BREVKODE_KLAGESKJEMA_ANKE_ETTERSENDELSE = "NAVe 90-00.08 A"
        private const val PDF_CODE = "PDF"
        private const val PDFA_CODE = "PDFA"

        private const val BEHANDLINGSTEMA_LONNSKOMPENSASJON = "ab0438"
        private const val BEHANDLINGSTEMA_TILBAKEBETALING_FORSKUDD = "ab0451"
        private const val BEHANDLINGSTEMA_ENGANGSSTONAD = "ab0327"
        private const val BEHANDLINGSTEMA_FORELDREPENGER = "ab0326"
        private const val BEHANDLINGSTEMA_SVANGERSKAPSPENGER = "ab0126"
        private const val BEHANDLINGSTEMA_FORERHUND = "ab0046"
        private const val BEHANDLINGSTEMA_SERVICEHUND = "ab0332"
        private const val BEHANDLINGSTEMA_ORTOPEDISKE_HJELPEMIDLER = "ab0013"
        private const val BEHANDLINGSTEMA_FOLKEHOYSKOLE = "ab0368"
        private const val BEHANDLINGSTEMA_HOREAPPARAT = "ab0243"

        val ourJacksonObjectMapper = jacksonObjectMapper()
    }

    fun createJournalpostAsSystemUser(
        klageAnkeInput: KlageAnkeInput,
    ): JournalpostResponse {
        val partialJournalpostWithoutDocuments = createPartialJournalpostWithoutDocuments(
            klageAnkeInput = klageAnkeInput,
        )

        val partialJournalpostAsJson = ourJacksonObjectMapper.writeValueAsString(partialJournalpostWithoutDocuments)
        val partialJournalpostAppendable = partialJournalpostAsJson.substring(0, partialJournalpostAsJson.length - 1)
        val journalpostRequestAsFile = Files.createTempFile(null, null)
        val journalpostRequestAsFileOutputStream = FileOutputStream(journalpostRequestAsFile.toFile())
        journalpostRequestAsFileOutputStream.write(partialJournalpostAppendable.toByteArray())

        //add documents (base64 encoded) to the request
        journalpostRequestAsFileOutputStream.write(",\"dokumenter\":[".toByteArray())

        writeDocumentsToJournalpostRequestAsFile(
            klageAnkeInput = klageAnkeInput,
            journalpostRequestAsFileOutputStream = journalpostRequestAsFileOutputStream,
        )

        journalpostRequestAsFileOutputStream.write("]}".toByteArray())
        journalpostRequestAsFileOutputStream.flush()

        return joarkClient.createJournalpostInJoarkAsSystemUser(
            journalpostRequestAsFile = journalpostRequestAsFile.toFile(),
        )
    }

    fun createPartialJournalpostWithoutDocuments(
        klageAnkeInput: KlageAnkeInput,
    ): JournalpostPartial {
        val innsendingsytelse = Innsendingsytelse.of(klageAnkeInput.innsendingsYtelseId)
        val tema = innsendingsytelseToTema[innsendingsytelse]!!.name
        val partialJournalpostWithoutDocuments = JournalpostPartial(
            tema = tema,
            behandlingstema = getBehandlingstema(innsendingsytelse = Innsendingsytelse.of(klageAnkeInput.innsendingsYtelseId)),
            avsenderMottaker = AvsenderMottaker(
                id = klageAnkeInput.identifikasjonsnummer,
                navn = getFullName(klageAnkeInput),
            ),
            sak = getSak(klageAnkeInput),
            tittel = when (klageAnkeInput.klageAnkeType) {
                KlageAnkeType.KLAGE -> KLAGE_TITTEL
                KlageAnkeType.ANKE -> ANKE_TITTEL
                KlageAnkeType.KLAGE_ETTERSENDELSE -> KLAGE_ETTERSENDELSE_TITTEL
                KlageAnkeType.ANKE_ETTERSENDELSE -> ANKE_ETTERSENDELSE_TITTEL
            },
            bruker = Bruker(
                id = klageAnkeInput.identifikasjonsnummer,
            ),
            tilleggsopplysninger = listOf(
                Tilleggsopplysning(nokkel = KLAGE_ANKE_ID_KEY, verdi = klageAnkeInput.id),
                Tilleggsopplysning(
                    nokkel = KLAGE_ANKE_YTELSE_KEY,
                    verdi = innsendingsytelse.name
                )
            ),
            eksternReferanseId = "${klageAnkeInput.klageAnkeType.name}_${klageAnkeInput.id}",
            journalfoerendeEnhet = getJournalfoerendeEnhetOverride(
                klageAnkeType = klageAnkeInput.klageAnkeType,
                identifikasjonsnummer = klageAnkeInput.identifikasjonsnummer,
                innsendingsytelse = innsendingsytelse,
                ettersendelseToKA = klageAnkeInput.ettersendelseTilKa ?: false,
            )
        )

        return partialJournalpostWithoutDocuments
    }

    private fun writeDocumentsToJournalpostRequestAsFile(
        klageAnkeInput: KlageAnkeInput,
        journalpostRequestAsFileOutputStream: FileOutputStream,
    ) {
        val tittel = when (klageAnkeInput.klageAnkeType) {
            KlageAnkeType.KLAGE -> KLAGE_TITTEL
            KlageAnkeType.ANKE -> ANKE_TITTEL
            KlageAnkeType.KLAGE_ETTERSENDELSE -> KLAGE_ETTERSENDELSE_TITTEL
            KlageAnkeType.ANKE_ETTERSENDELSE -> ANKE_ETTERSENDELSE_TITTEL
        }
        val brevkode = when (klageAnkeInput.klageAnkeType) {
            KlageAnkeType.KLAGE -> BREVKODE_KLAGESKJEMA_KLAGE
            KlageAnkeType.ANKE -> BREVKODE_KLAGESKJEMA_ANKE
            KlageAnkeType.KLAGE_ETTERSENDELSE -> BREVKODE_KLAGESKJEMA_KLAGE_ETTERSENDELSE
            KlageAnkeType.ANKE_ETTERSENDELSE -> BREVKODE_KLAGESKJEMA_ANKE_ETTERSENDELSE
        }

        val mellomlagretHovedDokument = MellomlagretDokument(
            title = tittel,
            file = pdfGenerator.generatePDF(klageAnkeInput),
            contentType = MediaType.APPLICATION_PDF,
        )

        val mellomlagretDokumenter = mutableListOf<MellomlagretDokument>()

        //Download attachments from temporary storage
        val vedleggAsMellomlagretDokument = klageAnkeInput.vedlegg.map { vedlegg ->
            MellomlagretDokument(
                title = vedlegg.tittel,
                file = fileClient.getAttachment(vedlegg.ref),
                contentType = MediaType.APPLICATION_PDF,
            )
        }

        mellomlagretDokumenter += mellomlagretHovedDokument
        mellomlagretDokumenter += vedleggAsMellomlagretDokument

        mellomlagretDokumenter.forEachIndexed { index, dokument ->
            val base64File = Files.createTempFile(null, null).toFile()
            encodeFileToBase64(dokument.file, base64File)

            val base64FileInputStream = FileInputStream(base64File)

            journalpostRequestAsFileOutputStream.write(
                """
                {
                    "tittel": ${ourJacksonObjectMapper.writeValueAsString(dokument.title)},
                    "brevkode": "$brevkode",
                    "dokumentvarianter": [
                        {
                            "filnavn":${ourJacksonObjectMapper.writeValueAsString(dokument.title)},
                            "filtype": $PDF_CODE,
                            "variantformat": "ARKIV",
                            "fysiskDokument": "
                """.toByteArray()
            )

            base64FileInputStream.use { input ->
                val buffer = ByteArray(1024) // Use a buffer size of 1K
                var length: Int
                while (input.read(buffer).also { length = it } != -1) {
                    journalpostRequestAsFileOutputStream.write(buffer, 0, length)
                }
            }
            journalpostRequestAsFileOutputStream.write("\"}]}".toByteArray())
            if (index < mellomlagretDokumenter.size - 1) {
                journalpostRequestAsFileOutputStream.write(",".toByteArray())
            }

            base64File.delete()
            dokument.file.delete()
        }

    }

    private fun encodeFileToBase64(sourceFile: File, destinationFile: File) {
        val sourceFileInputStream = FileInputStream(sourceFile)
        val destinationFileOutputStream = FileOutputStream(destinationFile)
        val encoder = Base64.getEncoder().wrap(destinationFileOutputStream)

        BufferedInputStream(sourceFileInputStream).use { input ->
            val buffer = ByteArray(3 * 1024) // Use a buffer size of 3K for example
            var length: Int
            while (input.read(buffer).also { length = it } != -1) {
                encoder.write(buffer, 0, length)
            }
        }

        encoder.close()

        destinationFileOutputStream.close()
    }

    private fun getJournalfoerendeEnhetOverride(
        klageAnkeType: KlageAnkeType,
        identifikasjonsnummer: String,
        innsendingsytelse: Innsendingsytelse?,
        ettersendelseToKA: Boolean,
    ): String? {
        val adressebeskyttelse =
            pdlClient.getPersonAdresseBeskyttelse(fnr = identifikasjonsnummer).data?.hentPerson?.adressebeskyttelse

        if (adressebeskyttelse?.any {
                it.gradering == PdlPerson.Adressebeskyttelse.GraderingType.STRENGT_FORTROLIG
                        || it.gradering == PdlPerson.Adressebeskyttelse.GraderingType.STRENGT_FORTROLIG_UTLAND
            } == true) {
            return null
        }

        return if (shouldBeSentToKA(klageAnkeType = klageAnkeType, ettersendelseToKA = ettersendelseToKA)) {
            if (innsendingsytelse != null) {
                innsendingsytelseToAnkeEnhet[innsendingsytelse]!!.navn
            } else null
        } else null
    }

    private fun shouldBeSentToKA(klageAnkeType: KlageAnkeType, ettersendelseToKA: Boolean): Boolean {
        return (klageAnkeType == KlageAnkeType.KLAGE_ETTERSENDELSE && ettersendelseToKA) ||
                (klageAnkeType in listOf(KlageAnkeType.ANKE, KlageAnkeType.ANKE_ETTERSENDELSE))
    }


    private fun getFullName(klageAnkeInput: KlageAnkeInput): String {
        return if (klageAnkeInput.mellomnavn.isBlank()) {
            "${klageAnkeInput.fornavn} ${klageAnkeInput.etternavn}"
        } else {
            "${klageAnkeInput.fornavn} ${klageAnkeInput.mellomnavn} ${klageAnkeInput.etternavn}"
        }
    }

    private fun getSak(klageAnkeInput: KlageAnkeInput): Sak? {
        val isInternalFORSak =
            if (innsendingsytelseToTema[Innsendingsytelse.of(klageAnkeInput.innsendingsYtelseId)] == Tema.FOR) {
                klageAnkeInput.internalSaksnummer?.toIntOrNull() != null
            } else {
                false
            }

        return if (isInternalFORSak) {
            Sak(
                sakstype = Sakstype.FAGSAK,
                fagsaksystem = FagsaksSystem.FS36,
                fagsakid = klageAnkeInput.internalSaksnummer,
            )
        } else if (klageAnkeInput.sak != null) { //this logic will take over for all cases when FOR sends us complete sak data.
            try {
                Sak(
                    sakstype = Sakstype.valueOf(klageAnkeInput.sak.sakstype),
                    fagsaksystem = FagsaksSystem.valueOf(klageAnkeInput.sak.fagsaksystem),
                    fagsakid = klageAnkeInput.sak.fagsakid,
                )
            } catch (e: Exception) {
                logger.error(
                    "Error when trying to parse sak from KlageAnkeInput: ${klageAnkeInput.sak}. Not using sak info for journalføring.",
                    e
                )
                null
            }
        } else {
            null
        }
    }

    fun getBehandlingstema(innsendingsytelse: Innsendingsytelse): String? {
        return when (innsendingsytelse) {
            Innsendingsytelse.LONNSKOMPENSASJON -> BEHANDLINGSTEMA_LONNSKOMPENSASJON
            Innsendingsytelse.DAGPENGER_TILBAKEBETALING_FORSKUDD -> BEHANDLINGSTEMA_TILBAKEBETALING_FORSKUDD
            Innsendingsytelse.FORELDREPENGER -> BEHANDLINGSTEMA_FORELDREPENGER
            Innsendingsytelse.ENGANGSSTONAD -> BEHANDLINGSTEMA_ENGANGSSTONAD
            Innsendingsytelse.SVANGERSKAPSPENGER -> BEHANDLINGSTEMA_SVANGERSKAPSPENGER
            Innsendingsytelse.FORERHUND -> BEHANDLINGSTEMA_FORERHUND
            Innsendingsytelse.SERVICEHUND -> BEHANDLINGSTEMA_SERVICEHUND
            Innsendingsytelse.ORTOPEDISKE_HJELPEMIDLER -> BEHANDLINGSTEMA_ORTOPEDISKE_HJELPEMIDLER
            Innsendingsytelse.FOLKEHOYSKOLE_ELLER_TILPASNINGSKURS -> BEHANDLINGSTEMA_FOLKEHOYSKOLE
            Innsendingsytelse.HOREAPPARAT_ELLER_TINNITUSMASKERER -> BEHANDLINGSTEMA_HOREAPPARAT
            else -> null
        }
    }

    private data class MellomlagretDokument(
        val title: String,
        val file: File,
        val contentType: MediaType,
    )
}