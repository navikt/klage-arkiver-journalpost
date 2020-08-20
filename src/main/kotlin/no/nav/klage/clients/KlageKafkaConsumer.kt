package no.nav.klage.clients

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.klage.domain.Klage
import no.nav.klage.getLogger
import no.nav.klage.getSecureLogger
import no.nav.klage.service.ApplicationService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class KlageKafkaConsumer(
    private val applicationService: ApplicationService,
    private val slackClient: SlackClient
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
        private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    }

    @KafkaListener(topics = ["\${KAFKA_TOPIC}"])
    fun listen(klageRecord: ConsumerRecord<String, String>) {
        logger.debug("Klage received from Kafka topic")
        secureLogger.debug("Klage received from Kafka topic: {}", klageRecord.value())

        runCatching {
            val klage = klageRecord.value().toKlage()
            slackClient.post(klage.id, "Ny klage mottatt.")
            logger.debug("Received klage has id: {}", klage.id)
            secureLogger.debug("Received klage has id: {} and fnr: {}", klage.id, klage.identifikasjonsnummer)
            applicationService.createJournalpost(klage)
        }.onFailure {
            slackClient.post("Nylig mottatt klage feilet!", Severity.ERROR)
            secureLogger.error("Failed to process klage", it)
        }
    }

    private fun String.toKlage(): Klage = mapper.readValue(this, Klage::class.java)

}
