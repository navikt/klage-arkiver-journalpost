package no.nav.klage.service

import no.nav.klage.clients.FileClient
import no.nav.klage.clients.JoarkClient
import no.nav.klage.clients.KlageDittnavAPIClient
import no.nav.klage.clients.PDFGeneratorClient
import no.nav.klage.common.KlageMetrics
import no.nav.klage.domain.KlageAnkeInput
import no.nav.klage.getLogger
import org.springframework.stereotype.Service

@Service
class ApplicationService(
        private val pdfGenerator: PDFGeneratorClient,
        private val fileClient: FileClient,
        private val joarkClient: JoarkClient,
        private val klageDittnavAPIClient: KlageDittnavAPIClient,
        private val klageMetrics: KlageMetrics,
        private val joarkService: JoarkService
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun createJournalpost(klageAnkeInput: KlageAnkeInput) {
        //Create PDF
        klageAnkeInput.fileContentAsBytes = pdfGenerator.generatePDF(klageAnkeInput)

        //Download attachments from temporary storage
        klageAnkeInput.vedlegg.forEach {
            it.fileContentAsBytes = fileClient.getAttachment(it.ref)
        }

        //Create journalpost and archive it
        val journalpostId = joarkService.createJournalpostInJoark(klageAnkeInput)

        //Record metrics
        klageMetrics.incrementKlagerArkivert()

        //Callback with journalpostId
        runCatching {
            if (klageAnkeInput.isKlage()) {
                klageDittnavAPIClient.setJournalpostIdToKlage(klageAnkeInput.id, journalpostId)
            } else {
                klageDittnavAPIClient.setJournalpostIdToAnke(klageAnkeInput.internalSaksnummer!!, journalpostId)
            }

        }.onFailure {
            logger.error("Could not call back to klage-api with journalpostId", it)
        }

        //Remove all attachments from the temporary storage
        klageAnkeInput.vedlegg.forEach {
            try {
                fileClient.deleteAttachment(it.ref)
            } catch (e: Exception) {
                logger.error("Could not delete attachment with id ${it.ref}", e)
            }
        }

        //Save klage-pdf in storage
        runCatching {
            klageAnkeInput.fileContentAsBytes?.let { fileClient.saveKlage(journalpostId, it) }
        }.onFailure {
            logger.error("Could not upload klage-pdf to file store.", it)
        }
    }
}
