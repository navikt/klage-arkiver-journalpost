package no.nav.klage.service

import no.nav.klage.clients.AttachmentClient
import no.nav.klage.clients.JoarkClient
import no.nav.klage.clients.PDFGeneratorClient
import no.nav.klage.common.KlageMetrics
import no.nav.klage.domain.Klage
import no.nav.klage.getLogger
import org.springframework.stereotype.Service

@Service
class ApplicationService(
    private val pdfGenerator: PDFGeneratorClient,
    private val attachmentClient: AttachmentClient,
    private val joarkClient: JoarkClient,
    private val klageMetrics: KlageMetrics
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun createJournalpost(klage: Klage) {
        //Create PDF
        klage.fileContentAsBytes = pdfGenerator.getFilledOutPDF(klage)

        //Download attachments from temporary storage
        klage.vedlegg.forEach {
            it.fileContentAsBytes = attachmentClient.getAttachment(it.ref)
        }

        //Create journalpost and archive it
        joarkClient.createJournalpost(klage)

        //Remove all attachments from the temporary storage
        klage.vedlegg.forEach {
            try {
                attachmentClient.deleteAttachment(it.ref)
            } catch (e: Exception) {
                logger.error("Could not delete attachment with id ${it.ref}", e)
            }
        }

        //Record metrics
        klageMetrics.incrementKlagerArkivert()
    }
}