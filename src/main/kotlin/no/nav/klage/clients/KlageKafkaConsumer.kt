package no.nav.klage.clients

import no.nav.klage.domain.KlageAnkeInput
import no.nav.klage.domain.toKlage
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
    }

    @KafkaListener(topics = ["\${KAFKA_TOPIC}"])
    fun listen(klageRecord: ConsumerRecord<String, String>) {
        logger.debug("Klage received from Kafka topic")
        secureLogger.debug("Klage received from Kafka topic: {}", klageRecord.value())

        runCatching {
            val klageAnke = klageRecord.value().toKlage()
            klageAnke.logIt()

            val journalpostIdResponse =
                if (klageAnke.isKlage()) {
                    klageDittnavAPIClient.getJournalpostForKlageId(klageAnke.id)
                } else {
                    klageDittnavAPIClient.getJournalpostForAnkeInternalSaksnummer(klageAnke.internalSaksnummer!!)
                }

            if (klageAnke.containsDeprecatedFields()) {
                slackClient.postMessage("Nylig mottatt innsending med id ${klageAnke.id} har utdatert modell.", Severity.ERROR)
                if (journalpostIdResponse.journalpostId == null) {
                    slackClient.postMessage("Innsending med id ${klageAnke.id} har ikke journalpostId. Undersøk dette nærmere!", Severity.ERROR)
                }
                return
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
            slackClient.postMessage("Nylig mottatt klage feilet! (${causeClass(rootCause(it))})", Severity.ERROR)
            secureLogger.error("Failed to process klage", it)
            throw RuntimeException("Could not process klage. See more details in secure log.")
        }
    }

    private fun KlageAnkeInput.logIt() {
        val klageid = this.id.toString()
        if (this.isKlage()) {
            when {
                this.isDagpengerVariant() -> {
                    slackClient.postMessage(
                        String.format(
                            "Klage (%s) med id <%s|%s> mottatt.",
                            this.ytelse,
                            Kibana.createUrl(klageid),
                            klageid
                        )
                    )
                }
                else -> {
                    slackClient.postMessage(
                        String.format(
                            "Klage med id <%s|%s> mottatt.",
                            Kibana.createUrl(klageid),
                            klageid
                        )
                    )
                }
            }
        } else {
            slackClient.postMessage(
                String.format(
                    "Anke med id <%s|%s> mottatt.",
                    this.internalSaksnummer?.let { Kibana.createUrl(it) },
                    this.internalSaksnummer
                )
            )
        }

        logger.debug("Received klage has id: {}", this.id)
        secureLogger.debug("Received klage has id: {} and fnr: {}", this.id, this.identifikasjonsnummer)
    }

    private fun rootCause(t: Throwable): Throwable = t.cause?.run { rootCause(this) } ?: t

    private fun causeClass(t: Throwable) = t.stackTrace?.firstOrNull()?.className ?: ""
}
