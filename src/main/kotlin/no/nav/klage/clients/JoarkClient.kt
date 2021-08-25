package no.nav.klage.clients

import brave.Tracer
import no.nav.klage.domain.Journalpost
import no.nav.klage.domain.JournalpostResponse
import no.nav.klage.getLogger
import no.nav.klage.getSecureLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient


@Component
class JoarkClient(
    private val joarkWebClient: WebClient,
    private val stsClient: StsClient
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @Value("\${DRY_RUN}")
    private lateinit var dryRun: String

    fun postJournalpost(journalpost: Journalpost, klageAnkeId: String): String {
        logger.debug("Posting journalpost to Joark.")

        return if (dryRun.toBoolean()) {
            logger.debug("Dry run activated. Not sending journalpost to Joark.")
            "dryRun, no journalpostId"
        } else {
            logger.debug("Posting journalpost to Joark.")
            val journalpostResponse = joarkWebClient.post()
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${stsClient.oidcToken()}")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(journalpost)
                .retrieve()
                .bodyToMono(JournalpostResponse::class.java)
                .block()
                ?: throw RuntimeException("Journalpost could not be created for klageAnke with id ${klageAnkeId}.")

            logger.debug("Journalpost successfully created in Joark with id {}.", journalpostResponse.journalpostId)

            journalpostResponse.journalpostId
        }
    }
}