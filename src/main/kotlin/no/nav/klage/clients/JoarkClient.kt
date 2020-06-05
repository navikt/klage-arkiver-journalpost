package no.nav.klage.clients

import no.nav.klage.domain.*
import no.nav.klage.getLogger
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.util.*


@Component
class JoarkClient(private val joarkWebClient: WebClient, private val stsClient: StsClient) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)

        private const val KLAGE_TITTEL = "Klage/Anke"
        private const val BREVKODE_KLAGESKJEMA = "NAV 90-00.08"
    }

    fun createJournalpost(klage: Klage) {
        logger.debug("Creating journalpost.")
        val journalpost = getJournalpost(klage)

        val mono = joarkWebClient.post()
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${stsClient.oidcToken()}")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(journalpost)
            .retrieve().bodyToMono(String::class.java)

        logger.debug("Mono result: {}", mono.block())

        logger.debug("Journalpost successfully created in Joark.")
    }

    private fun getJournalpost(klage: Klage): Journalpost =
        Journalpost(
            tema = klage.tema,
            avsenderMottaker = AvsenderMottaker(
                id = klage.foedselsnummer,
                idType = "FNR",
                navn = klage.navn
            ),
            tittel = KLAGE_TITTEL,
            bruker = Bruker(
                id = klage.foedselsnummer,
                idType = "FNR"
            ),
            dokumenter = getDokumenter(klage)
        )

    private fun getDokumenter(klage: Klage): List<Dokument> {
        val hovedDokument = Dokument(
            tittel = KLAGE_TITTEL,
            brevkode = BREVKODE_KLAGESKJEMA,
            dokumentVarianter = getDokumentVariant(klage.fileContentAsBytes, "PDFA")
        )
        val documents = mutableListOf(hovedDokument)

        klage.vedlegg.forEach {
            val doc = Dokument(
                tittel = it.tittel,
                dokumentVarianter = getDokumentVariant(klage.fileContentAsBytes, it.type)
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
}