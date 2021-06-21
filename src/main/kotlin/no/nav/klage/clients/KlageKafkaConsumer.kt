package no.nav.klage.clients

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.klage.domain.Klage
import no.nav.klage.getLogger
import no.nav.klage.getSecureLogger
import no.nav.klage.service.ApplicationService
import no.nav.slackposter.Kibana
import no.nav.slackposter.Severity
import no.nav.slackposter.SlackClient
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

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
        private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    }

    @KafkaListener(topics = ["\${KAFKA_TOPIC}"])
    fun listen(klageRecord: ConsumerRecord<String, String>) {
        logger.debug("Klage received from Kafka topic")
        secureLogger.debug("Klage received from Kafka topic: {}", klageRecord.value())

        runCatching {
            val klage = klageRecord.value().toKlage()
            klage.logIt()
            val journalpostIdResponse = klageDittnavAPIClient.getJournalpostForKlageId(klage.id)

            if (journalpostIdResponse.journalpostId != null) {
                logger.info("Klage with ID {} is already registered in Joark with journalpost ID {}. Ignoring.", klage.id, journalpostIdResponse.journalpostId)
                return
            }

            applicationService.createJournalpost(klage)
        }.onFailure {
            slackClient.postMessage("Nylig mottatt klage feilet! (${causeClass(rootCause(it))})", Severity.ERROR)
            secureLogger.error("Failed to process klage", it)
            throw RuntimeException("Could not process klage. See more details in secure log.")
        }
    }

    private fun String.toKlage(): Klage = mapper.readValue(this, Klage::class.java)

    private fun Klage.logIt() {
        val klageid = this.id.toString()
        when {
            this.isLonnskompensasjon() -> {
                slackClient.postMessage(String.format("Klage (Lønnskompenssasjon for permitterte) med id <%s|%s> mottatt.", Kibana.createUrl(klageid), klageid))
            }
            this.isTilbakebetalingAvForskuddPaaDagpenger() -> {
                slackClient.postMessage(String.format("Klage (Tilbakebetaling av forskudd på dagpenger) med id <%s|%s> mottatt.", Kibana.createUrl(klageid), klageid))
            }
            this.isFeriepengerAvDagpenger() -> {
                slackClient.postMessage(String.format("Klage (Feriepenger av dagpenger) med id <%s|%s> mottatt.", Kibana.createUrl(klageid), klageid))
            }
            else -> {
                slackClient.postMessage(String.format("Klage med id <%s|%s> mottatt.", Kibana.createUrl(klageid), klageid))
            }
        }

        logger.debug("Received klage has id: {}", this.id)
        secureLogger.debug("Received klage has id: {} and fnr: {}", this.id, this.identifikasjonsnummer)
    }

    private fun rootCause(t: Throwable): Throwable = t.cause?.run { rootCause(this) } ?: t

    private fun causeClass(t: Throwable) = t.stackTrace?.firstOrNull()?.className ?: ""
}
