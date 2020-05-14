package no.nav.klage.service

import no.nav.klage.clients.GCPStorageClient
import no.nav.klage.clients.JoarkClient
import no.nav.klage.clients.PDFGeneratorClient
import no.nav.klage.domain.Klage
import org.springframework.stereotype.Service

@Service
class AppService(
    private val pdfGenerator: PDFGeneratorClient,
    private val gcpStorageClient: GCPStorageClient,
    private val joarkClient: JoarkClient
) {

    fun createJournalpost(klage: Klage) {
        //Create PDF
        val filledOutPDFBytes = pdfGenerator.getFilledOutPDF(klage)

        //Download attachments from GCP Storage Bucket
        if (klage.attachments.isNotEmpty()) {
            gcpStorageClient.getAttachments(klage.attachments)
        }

        //Create journalpost and archive it
        joarkClient.createJournalpost(filledOutPDFBytes, klage)
    }
}