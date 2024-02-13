package no.nav.klage.clients

import no.nav.klage.domain.KlageAnkeType
import no.nav.klage.domain.toKlageAnkeInput
import no.nav.klage.getLogger
import no.nav.klage.getSecureLogger
import no.nav.klage.service.ApplicationService
import no.nav.slackposter.Severity
import no.nav.slackposter.SlackClient
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException

@Component
class KlageKafkaConsumer(
    private val applicationService: ApplicationService,
    private val slackClient: SlackClient,
    private val klageDittnavAPIClient: KlageDittnavAPIClient
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    @Value("\${NAIS_CLUSTER_NAME}")
    lateinit var naisCluster: String

    @KafkaListener(topics = ["\${KAFKA_TOPIC}"])
    fun listen(klageRecord: ConsumerRecord<String, String>) {
        logger.debug("Klage received from Kafka topic")
        secureLogger.debug("Klage received from Kafka topic: {}", klageRecord.value())

        runCatching {
            val klageAnke = klageRecord.value().toKlageAnkeInput()
            val journalpostIdResponse =
                try {
                    when (klageAnke.klageAnkeType) {
                        KlageAnkeType.KLAGE -> klageDittnavAPIClient.getJournalpostForKlageId(klageAnke.id)
                        KlageAnkeType.ANKE -> klageDittnavAPIClient.getJournalpostForAnkeId(klageAnke.id)
                        KlageAnkeType.KLAGE_ETTERSENDELSE -> TODO()
                        KlageAnkeType.ANKE_ETTERSENDELSE -> TODO()
                    }
                } catch (e: WebClientResponseException.NotFound) {
                    slackClient.postMessage(
                        "Innsending med id ${klageAnke.id} fins ikke i klage-dittnav-api. Undersøk dette nærmere!",
                        Severity.ERROR
                    )
                    logger.error("Input has id not found in klage-dittnav-api. See more details in secure log.")
                    secureLogger.error("Input has id not found in klage-dittnav-api,", klageAnke)
                    if (naisCluster == "dev-gcp") {
                        return
                    } else {
                        throw RuntimeException("Input not found in klage-dittnav-api!")
                    }
                }

            if (journalpostIdResponse.journalpostId != null) {
                logger.info(
                    "Klage with ID {} is already registered in Joark with journalpost ID {}. Ignoring.",
                    klageAnke.id,
                    journalpostIdResponse.journalpostId
                )
                return
            }

            applicationService.createJournalpost(klageAnke)
        }.onFailure {
            slackClient.postMessage("Nylig mottatt innsending feilet! (${causeClass(rootCause(it))})", Severity.ERROR)
            secureLogger.error("Failed to process innsending", it)
            throw RuntimeException("Could not process innsending. See more details in secure log.")
        }
    }

    private fun rootCause(t: Throwable): Throwable = t.cause?.run { rootCause(this) } ?: t

    private fun causeClass(t: Throwable) = t.stackTrace?.firstOrNull()?.className ?: ""
}
