package no.nav.klage.service

import no.nav.klage.clients.FileClient
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
        klageDittnavAPIClient.setJournalpostIdInKlanke(
            klankeId = klageAnkeInput.id,
            journalpostId = journalpostId
        )

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
    }
}
