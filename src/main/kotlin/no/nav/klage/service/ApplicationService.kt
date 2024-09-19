package no.nav.klage.service

import no.nav.klage.clients.FileClient
import no.nav.klage.clients.KlageDittnavAPIClient
import no.nav.klage.clients.PDFGeneratorClient
import no.nav.klage.common.KlageMetrics
import no.nav.klage.domain.KlageAnkeInput
import no.nav.klage.domain.KlageAnkeType
import no.nav.klage.domain.MellomlagretDokument
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
        private const val KLAGE_TITTEL = "Klage"
        private const val KLAGE_ETTERSENDELSE_TITTEL = "Ettersendelse til klage"
        private const val ANKE_TITTEL = "Anke"
        private const val ANKE_ETTERSENDELSE_TITTEL = "Ettersendelse til anke"
    }

    fun createJournalpost(klageAnkeInput: KlageAnkeInput) {
        //Create PDF
        klageAnkeInput.hoveddokument = getMellomlagretHoveddokument(klageAnkeInput)

        //Download attachments from temporary storage
        klageAnkeInput.vedlegg.forEach {
            it.file = fileClient.getAttachment(id = it.ref!!)
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
                fileClient.deleteAttachment(it.ref!!)
            } catch (e: Exception) {
                logger.error("Could not delete attachment with id ${it.ref}", e)
            }
        }
    }

    private fun getMellomlagretHoveddokument(klageAnkeInput: KlageAnkeInput): MellomlagretDokument {
        return MellomlagretDokument(
            tittel = when (klageAnkeInput.klageAnkeType) {
                KlageAnkeType.KLAGE -> KLAGE_TITTEL
                KlageAnkeType.ANKE -> ANKE_TITTEL
                KlageAnkeType.KLAGE_ETTERSENDELSE -> KLAGE_ETTERSENDELSE_TITTEL
                KlageAnkeType.ANKE_ETTERSENDELSE -> ANKE_ETTERSENDELSE_TITTEL
            },
            ref = null,
            file = pdfGenerator.generatePDF(klageAnkeInput),
        )        
    }
}
