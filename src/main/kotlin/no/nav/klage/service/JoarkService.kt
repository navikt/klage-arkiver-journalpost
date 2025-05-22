package no.nav.klage.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.klage.clients.JoarkClient
import no.nav.klage.clients.pdl.PdlClient
import no.nav.klage.clients.pdl.PdlPerson
import no.nav.klage.domain.*
import no.nav.klage.getLogger
import no.nav.klage.kodeverk.Tema
import no.nav.klage.kodeverk.innsendingsytelse.Innsendingsytelse
import no.nav.klage.kodeverk.innsendingsytelse.innsendingsytelseToAnkeEnhet
import no.nav.klage.kodeverk.innsendingsytelse.innsendingsytelseToTema
import org.springframework.stereotype.Service
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
    }

    fun createJournalpostInJoark(klageAnkeInput: KlageAnkeInput): String {
        logger.debug("Creating journalpost.")

        val journalpostRequest = createJournalpostRequest(klageAnkeInput)

        return joarkClient.postJournalpost(journalpostRequest, klageAnkeInput.id)
    }

    private fun createJournalpostRequest(klageAnkeInput: KlageAnkeInput): Journalpost {
        val innsendingsytelse = Innsendingsytelse.of(klageAnkeInput.innsendingsYtelseId)
        val tema = innsendingsytelseToTema[innsendingsytelse]!!.name

        return Journalpost(
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
            dokumenter = getDokumenter(klageAnkeInput),
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
        val isInternalFORSak = if (innsendingsytelseToTema[Innsendingsytelse.of(klageAnkeInput.innsendingsYtelseId)] == Tema.FOR) {
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
                logger.error("Error when trying to parse sak from KlageAnkeInput: ${klageAnkeInput.sak}. Not using sak info for journalføring.", e)
                null
            }
        } else {
            null
        }
    }

    private fun getDokumenter(klageAnkeInput: KlageAnkeInput): List<Dokument> {
        val hovedDokument = Dokument(
            tittel = when (klageAnkeInput.klageAnkeType) {
                KlageAnkeType.KLAGE -> KLAGE_TITTEL
                KlageAnkeType.ANKE -> ANKE_TITTEL
                KlageAnkeType.KLAGE_ETTERSENDELSE -> KLAGE_ETTERSENDELSE_TITTEL
                KlageAnkeType.ANKE_ETTERSENDELSE -> ANKE_ETTERSENDELSE_TITTEL
            },
            brevkode = when (klageAnkeInput.klageAnkeType) {
                KlageAnkeType.KLAGE -> BREVKODE_KLAGESKJEMA_KLAGE
                KlageAnkeType.ANKE -> BREVKODE_KLAGESKJEMA_ANKE
                KlageAnkeType.KLAGE_ETTERSENDELSE -> BREVKODE_KLAGESKJEMA_KLAGE_ETTERSENDELSE
                KlageAnkeType.ANKE_ETTERSENDELSE -> BREVKODE_KLAGESKJEMA_ANKE_ETTERSENDELSE
            },
            //Don't perform pdfa-check for now. Issues with compatibility with Vera and Spring Boot 3.
            dokumentVarianter = getDokumentVariant(klageAnkeInput.fileContentAsBytes, performPdfaCheck = false)
        )
        val documents = mutableListOf(hovedDokument)

        klageAnkeInput.vedlegg.forEach {
            //Attachments will always be PDF as of now.
            logger.debug("Adding attachment with GCS reference ${it.ref} to journalpost")
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
}