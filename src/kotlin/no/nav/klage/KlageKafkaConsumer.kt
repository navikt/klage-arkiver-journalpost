package no.nav.klage

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class KlageKafkaConsumer {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @KafkaListener(topics = ["\${KAFKA_TOPIC}"])
    fun listen(record: ConsumerRecord<String, String>) {
        logger.info(record.toString())
    }
}
