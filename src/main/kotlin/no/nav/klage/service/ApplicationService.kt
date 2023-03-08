package no.nav.klage.service

import no.nav.klage.clients.FileClient
import no.nav.klage.clients.KlageDittnavAPIClient
import no.nav.klage.clients.PDFGeneratorClient
import no.nav.klage.common.KlageMetrics
import no.nav.klage.domain.KlageAnkeInput
import no.nav.klage.domain.KlageAnkeType
import no.nav.klage.getLogger
import org.springframework.stereotype.Service

@Service
class ApplicationService(
    private val pdfGenerator: PDFGeneratorClient,
    private val fileClient: FileClient,
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
        //If duplicate, we still get journalpostId and continue
        val journalpostId = joarkService.createJournalpostInJoark(klageAnkeInput)

        //Callback with journalpostId
        when (klageAnkeInput.klageAnkeType) {
            KlageAnkeType.KLAGE -> klageDittnavAPIClient.setJournalpostIdInKlage(klageAnkeInput.id, journalpostId)
            KlageAnkeType.ANKE -> klageDittnavAPIClient.setJournalpostIdInAnke(klageAnkeInput.id, journalpostId)
        }

        //Record metrics
        klageMetrics.incrementKlagerArkivert()

        //Remove all attachments from the temporary storage
        klageAnkeInput.vedlegg.forEach {
            try {
                fileClient.deleteAttachment(it.ref)
            } catch (e: Exception) {
                logger.error("Could not delete attachment with id ${it.ref}", e)
            }
        }

        //Save klage/anke-pdf in storage
        runCatching {
            klageAnkeInput.fileContentAsBytes?.let { fileClient.saveKlageAnke(journalpostId, it) }
        }.onFailure {
            logger.error("Could not upload klage/anke-pdf to file store.", it)
        }
    }
}
