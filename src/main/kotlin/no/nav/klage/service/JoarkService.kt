package no.nav.klage.service

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.klage.clients.JoarkClient
import no.nav.klage.clients.pdl.PdlClient
import no.nav.klage.clients.pdl.PdlPerson
import no.nav.klage.domain.*
import no.nav.klage.getLogger
import no.nav.klage.getSecureLogger
import no.nav.klage.kodeverk.Tema
import no.nav.klage.kodeverk.innsendingsytelse.Innsendingsytelse
import no.nav.klage.kodeverk.innsendingsytelse.innsendingsytelseToAnkeEnhet
import no.nav.klage.kodeverk.innsendingsytelse.innsendingsytelseToTema
import org.springframework.stereotype.Service
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.*

@Service
class JoarkService(
    private val joarkClient: JoarkClient,
    private val pdfService: PdfService,
    private val pdlClient: PdlClient,
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()

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

        val ourJacksonObjectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .setDateFormat(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS"))

    }

    fun createJournalpostInJoark(klageAnkeInput: KlageAnkeInput): String {
        logger.debug("Creating journalpost.")

        val partialJournalpostWithoutDocuments = createPartialJournalpostRequestWithoutDocuments(klageAnkeInput)

        val partialJournalpostAsJson = ourJacksonObjectMapper.writeValueAsString(partialJournalpostWithoutDocuments)

        secureLogger.debug("Journalpost without documents (what we post to dokarkiv/Joark): {}", partialJournalpostWithoutDocuments)

        val partialJournalpostAppendable = partialJournalpostAsJson.substring(0, partialJournalpostAsJson.length - 1)
        val journalpostRequestAsFile = Files.createTempFile(null, null)
        val journalpostRequestAsFileOutputStream = FileOutputStream(journalpostRequestAsFile.toFile())
        journalpostRequestAsFileOutputStream.write(partialJournalpostAppendable.toByteArray())

        //add documents (base64 encoded) to the request
        journalpostRequestAsFileOutputStream.write(",\"dokumenter\":[".toByteArray())

        writeDocumentsToJournalpostRequestAsFile(
            mellomlagretDokumenter = listOf(klageAnkeInput.hoveddokument!!) + klageAnkeInput.vedlegg,
            journalpostRequestAsFileOutputStream = journalpostRequestAsFileOutputStream,
            brevkode = when (klageAnkeInput.klageAnkeType) {
                KlageAnkeType.KLAGE -> BREVKODE_KLAGESKJEMA_KLAGE
                KlageAnkeType.ANKE -> BREVKODE_KLAGESKJEMA_ANKE
                KlageAnkeType.KLAGE_ETTERSENDELSE -> BREVKODE_KLAGESKJEMA_KLAGE_ETTERSENDELSE
                KlageAnkeType.ANKE_ETTERSENDELSE -> BREVKODE_KLAGESKJEMA_ANKE_ETTERSENDELSE
            }
        )

        journalpostRequestAsFileOutputStream.write("]}".toByteArray())
        journalpostRequestAsFileOutputStream.flush()

        return joarkClient.postJournalpost(
            journalpostRequestAsFile = journalpostRequestAsFile.toFile(),
            klageAnkeId = klageAnkeInput.id
        )
    }

    private fun createPartialJournalpostRequestWithoutDocuments(klageAnkeInput: KlageAnkeInput): JournalpostPartial {
        val tema = if (klageAnkeInput.innsendingsYtelseId.isNullOrBlank()) {
            klageAnkeInput.tema
        } else {
            innsendingsytelseToTema[Innsendingsytelse.of(klageAnkeInput.innsendingsYtelseId)]!!.name
        }

        val innsendingsytelse = klageAnkeInput.innsendingsYtelseId?.let { Innsendingsytelse.of(it) }

        return JournalpostPartial(
            tema = tema,
            behandlingstema = klageAnkeInput.getBehandlingstema(),
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
                    verdi = innsendingsytelse?.name ?: klageAnkeInput.ytelse
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
        val isInternalFORSak = if (klageAnkeInput.innsendingsYtelseId.isNullOrBlank()) {
            if (klageAnkeInput.tema == "FOR") {
                klageAnkeInput.internalSaksnummer?.toIntOrNull() != null
            } else false
        } else if (innsendingsytelseToTema[Innsendingsytelse.of(klageAnkeInput.innsendingsYtelseId)] == Tema.FOR) {
            klageAnkeInput.internalSaksnummer?.toIntOrNull() != null
        } else {
            false
        }

        return if (isInternalFORSak) {
            Sak(
                sakstype = Sakstype.FAGSAK,
                fagsaksystem = FagsaksSystem.FS36,
                fagsakid = klageAnkeInput.internalSaksnummer
            )
        } else {
            null
        }
    }

    private fun writeDocumentsToJournalpostRequestAsFile(
        mellomlagretDokumenter: List<MellomlagretDokument>,
        journalpostRequestAsFileOutputStream: FileOutputStream,
        brevkode: String,
    ) {
        mellomlagretDokumenter.forEachIndexed { index, dokument ->
            val base64File = Files.createTempFile(null, null).toFile()
            encodeFileToBase64(dokument.file, base64File)

            val base64FileInputStream = FileInputStream(base64File)

            journalpostRequestAsFileOutputStream.write("{\"tittel\":\"${dokument.tittel}\",\"brevkode\":\"$brevkode\",\"dokumentvarianter\":[{\"filnavn\":\"${dokument.tittel}\",\"filtype\":\"PDF\",\"variantformat\":\"ARKIV\",\"fysiskDokument\":\"".toByteArray())

            base64FileInputStream.use { input ->
                val buffer = ByteArray(1024) // Use a buffer size of 1K for example
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

        destinationFileOutputStream.close()
    }
}