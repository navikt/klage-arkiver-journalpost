package no.nav.klage

import no.nav.klage.domain.Klage
import no.nav.klage.service.AppService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class KlageKafkaConsumer(private val appService: AppService) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @KafkaListener(topics = ["\${KAFKA_TOPIC}"])
    fun listen(record: ConsumerRecord<String, Klage>) {
        logger.info("Klage received: $record")

        appService.createJournalpost(record.value())
    }
}
