package no.nav.klage.clients

import no.nav.klage.domain.JournalpostResponse
import no.nav.klage.getLogger
import no.nav.klage.util.TokenUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.io.File


@Component
class JoarkClient(
    private val joarkWebClient: WebClient,
    private val tokenUtil: TokenUtil,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @Value("\${DRY_RUN}")
    private lateinit var dryRun: String

    fun postJournalpost(
        journalpostRequestAsFile: File,
        klageAnkeId: String
    ): String {
        return if (dryRun.toBoolean()) {
            logger.debug("Dry run activated. Not sending journalpost to Joark.")
            "dryRun, no journalpostId"
        } else {
            logger.debug("Posting journalpost to Joark.")

            val dataBufferFactory = DefaultDataBufferFactory()
            val dataBuffer = DataBufferUtils.read(journalpostRequestAsFile.toPath(), dataBufferFactory, 256 * 256)

            val journalpostResponse = joarkWebClient.post()
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenUtil.getAppAccessTokenWithDokarkivScope()}")
                .contentType(MediaType.APPLICATION_JSON)
                .body(dataBuffer, DataBuffer::class.java)
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
            journalpostRequestAsFile.delete()

            journalpostResponse.journalpostId
        }
    }
}