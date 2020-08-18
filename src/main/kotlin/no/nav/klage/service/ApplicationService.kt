package no.nav.klage.service

import no.nav.klage.clients.FileClient
import no.nav.klage.clients.JoarkClient
import no.nav.klage.clients.KlageDittnavAPIClient
import no.nav.klage.clients.PDFGeneratorClient
import no.nav.klage.common.KlageMetrics
import no.nav.klage.domain.Klage
import no.nav.klage.getLogger
import org.springframework.stereotype.Service

@Service
class ApplicationService(
        private val pdfGenerator: PDFGeneratorClient,
        private val fileClient: FileClient,
        private val joarkClient: JoarkClient,
        private val klageDittnavAPIClient: KlageDittnavAPIClient,
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
            it.fileContentAsBytes = fileClient.getAttachment(it.ref)
        }

        //Create journalpost and archive it
        val journalpostId = joarkClient.createJournalpost(klage)

        klageDittnavAPIClient.setJournalpostIdToKlage(klage.id, journalpostId)

        //Remove all attachments from the temporary storage
        klage.vedlegg.forEach {
            try {
                fileClient.deleteAttachment(it.ref)
            } catch (e: Exception) {
                logger.error("Could not delete attachment with id ${it.ref}", e)
            }
        }

        //Record metrics
        klageMetrics.incrementKlagerArkivert()
    }
}