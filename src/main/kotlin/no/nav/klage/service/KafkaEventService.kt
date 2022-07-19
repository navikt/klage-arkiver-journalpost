package no.nav.klage.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.klage.domain.Event
import no.nav.klage.getLogger
import no.nav.klage.getSecureLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class KafkaEventService(
    private val aivenKafkaTemplate: KafkaTemplate<String, String>,
    @Value("\${ARKIVER_JOURNALPOST_EVENT_TOPIC}")
    private val arkiverJournalpostEventTopic: String,
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    fun publishEvent(event: Event) {
        runCatching {
            logger.debug("Publishing arkiverJournalpost event to Kafka for subscribers: {}", event)

            val result = aivenKafkaTemplate.send(
                arkiverJournalpostEventTopic,
                jacksonObjectMapper().writeValueAsString(event)
            ).get()
            logger.debug("Published arkiverJournalpost event to Kafka for subscribers: {}", result)
        }.onFailure {
            logger.error("Could not publish arkiverJournalpost event to subscribers", it)
        }
    }
}