package no.nav.klage.service

import no.nav.klage.clients.FileClient
import no.nav.klage.clients.KlageDittnavAPIClient
import no.nav.klage.common.KlageMetrics
import no.nav.klage.domain.KlageAnkeInput
import no.nav.klage.util.getLogger
import org.springframework.stereotype.Service

@Service
class ApplicationService(
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
        //Create journalpost and archive it
        //If duplicate, we still get journalpostId and continue
        val journalpostResponse = joarkService.createJournalpostAsSystemUser(klageAnkeInput)

        //Callback with journalpostId
        klageDittnavAPIClient.setJournalpostIdInKlanke(
            klankeId = klageAnkeInput.id,
            journalpostId = journalpostResponse.journalpostId,
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
