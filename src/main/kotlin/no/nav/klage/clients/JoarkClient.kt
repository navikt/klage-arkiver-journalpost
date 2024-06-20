package no.nav.klage.clients

import no.nav.klage.domain.Journalpost
import no.nav.klage.domain.JournalpostResponse
import no.nav.klage.getLogger
import no.nav.klage.util.TokenUtil
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono


@Component
class JoarkClient(
    private val joarkWebClient: WebClient,
    private val tokenUtil: TokenUtil,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun postJournalpost(journalpost: Journalpost, klageAnkeId: String): String {
        logger.debug("Posting journalpost to Joark.")
        val journalpostResponse = joarkWebClient.post()
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenUtil.getAppAccessTokenWithDokarkivScope()}")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(journalpost)
            .retrieve()
            .onStatus(HttpStatus.CONFLICT::equals) {
                logger.debug("Journalpost already exists. Returning journalpost id.")
                //Means that the flow continues
                Mono.empty()
            }
            .onStatus(HttpStatus.CREATED::equals) {
                logger.debug("Journalpost successfully created in Joark")
                Mono.empty()
            }
            .bodyToMono(JournalpostResponse::class.java)
            .block()
            ?: throw RuntimeException("Journalpost could not be created for klageAnke with id ${klageAnkeId}.")

        logger.debug("Returning journalpost id {}.", journalpostResponse.journalpostId)

        return journalpostResponse.journalpostId
    }
}