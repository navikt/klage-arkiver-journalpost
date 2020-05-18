package klage.clients

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import klage.domain.Klage
import klage.getLogger
import klage.service.ApplicationService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class KlageKafkaConsumer(private val applicationService: ApplicationService) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val mapper = jacksonObjectMapper()
    }

    @KafkaListener(topics = ["\${KAFKA_TOPIC}"])
    fun listen(klageRecord: ConsumerRecord<String, String>) {
        logger.debug("Klage received: {}", klageRecord)
        applicationService.createJournalpost(klageRecord.value().toJson())
    }

    private fun String.toJson(): Klage {
        return mapper.readValue(this, Klage::class.java)
    }
}
