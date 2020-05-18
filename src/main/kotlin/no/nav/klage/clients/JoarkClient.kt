package no.nav.klage.clients

import klage.domain.*
import no.nav.klage.domain.*
import no.nav.klage.getLogger
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.util.*


@Component
class JoarkClient(private val joarkWebClient: WebClient) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun createJournalpost(klage: Klage) {
        logger.debug("Creating journalpost.")
        val journalpost = getJournalpost(klage)

        joarkWebClient.post()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(journalpost)
            .retrieve()
    }

    private fun getJournalpost(klage: Klage): Journalpost =
        Journalpost(
            tema = klage.tema,
            avsenderMottaker = AvsenderMottaker(
                id = klage.foedselsnummer,
                idType = "FNR",
                navn = klage.navn
            ),
            tittel = "TODO: en klage",
            bruker = Bruker(
                id = klage.foedselsnummer,
                idType = "FNR"
            ),
            dokumenter = getDokumenter(klage)
        )

    private fun getDokumenter(klage: Klage): List<Dokument> {
        val hovedDokument = Dokument(
            tittel = "Hoveddokument klage",
            brevkode = "NAV 90-00.08", //brevkode for klageskjema
            dokumentVarianter = getDokumentVariant(klage.fileContentAsBytes, "PDFA", "ARKIV")
        )
        val documents = mutableListOf(hovedDokument)

        klage.vedlegg.forEach {
            val doc = Dokument(
                tittel = it.tittel,
                dokumentVarianter = getDokumentVariant(klage.fileContentAsBytes, "todo", "?")
            )
            documents.add(doc)
        }
        return documents
    }

    private fun getDokumentVariant(bytes: ByteArray?, fileType: String, variantformat: String): List<DokumentVariant> {
        val dokumentVariant = DokumentVariant(
            filtype = fileType,
            variantformat = variantformat,
            fysiskDokument = Base64.getEncoder().encodeToString(bytes)
        )
        return listOf(dokumentVariant)
    }
}