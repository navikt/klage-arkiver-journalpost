package klage.service

import klage.clients.GcsClient
import klage.clients.JoarkClient
import klage.clients.PDFGeneratorClient
import klage.domain.Klage
import org.springframework.stereotype.Service

@Service
class ApplicationService(
    private val pdfGenerator: PDFGeneratorClient,
    private val GcsClient: GcsClient,
    private val joarkClient: JoarkClient
) {

    fun createJournalpost(klage: Klage) {
        //Create PDF
        klage.fileContentAsBytes = pdfGenerator.getFilledOutPDF(klage)

        //Download attachments from GCP Storage Bucket
        klage.vedlegg.forEach {
            it.fileContentAsBytes = GcsClient.getAttachment(it.gcsRef)
        }

        //Create journalpost and archive it
        joarkClient.createJournalpost(klage)
    }
}