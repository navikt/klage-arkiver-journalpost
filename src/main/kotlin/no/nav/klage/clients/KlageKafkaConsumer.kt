package no.nav.klage.clients

import no.nav.klage.domain.toKlageAnkeInput
import no.nav.klage.service.ApplicationService
import no.nav.klage.util.getLogger
import no.nav.klage.util.getTeamLogger
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
        private val teamLogger = getTeamLogger()
    }

    @Value("\${NAIS_CLUSTER_NAME}")
    lateinit var naisCluster: String

    @KafkaListener(topics = ["\${KAFKA_TOPIC}"])
    fun listen(klageRecord: ConsumerRecord<String, String>) {
        logger.debug("Klanke received from Kafka topic")

        runCatching {
            val klageAnke = klageRecord.value().toKlageAnkeInput()
            logger.debug("Received klanke has id {}", klageAnke.id)

            val journalpostIdResponse =
                try {
                    klageDittnavAPIClient.getJournalpostForKlankeId(klageAnke.id)
                } catch (e: WebClientResponseException.NotFound) {
                    slackClient.postMessage(
                        "Innsending med id ${klageAnke.id} fins ikke i klage-dittnav-api. Undersøk dette nærmere!",
                        Severity.ERROR
                    )
                    logger.error("Klanke not found in klage-dittnav-api.")
                    if (naisCluster == "dev-gcp") {
                        return
                    } else {
                        throw RuntimeException("Input not found in klage-dittnav-api!")
                    }
                }

            if (journalpostIdResponse.journalpostId != null) {
                logger.info(
                    "Klanke with ID {} is already registered in Joark with journalpost ID {}. Ignoring.",
                    klageAnke.id,
                    journalpostIdResponse.journalpostId
                )
                return
            }

            applicationService.createJournalpost(klageAnke)
        }.onFailure {
            slackClient.postMessage("Nylig mottatt innsending feilet. Sjekk team-logs for detaljer.", Severity.ERROR)
            logger.error("Could not process innsending. See more details in team-logs.")
            teamLogger.error("Could not process innsending", it)
            throw RuntimeException("Could not process innsending")
        }
    }
}
