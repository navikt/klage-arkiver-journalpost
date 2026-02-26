package no.nav.klage.clients

import no.nav.klage.config.WebClientConfig.Companion.LARGE_FILE_UPLOAD_TIMEOUT_SECONDS
import no.nav.klage.config.WebClientConfig.Companion.SMALL_FILE_UPLOAD_TIMEOUT_SECONDS
import no.nav.klage.domain.JournalpostResponse
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.resilience.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.body
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.io.File


@Component
class JoarkClient(
    private val tokenUtil: TokenUtil,
    @Qualifier("joarkLargeFileWebClient") private val joarkLargeFileWebClient: WebClient,
    @Qualifier("joarkSmallFileWebClient") private val joarkSmallFileWebClient: WebClient,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        const val LARGE_FILE_THRESHOLD_BYTES = 5 * 1024 * 1024L
    }

    @Value("\${DRY_RUN}")
    private lateinit var dryRun: String

    @Retryable
    fun createJournalpostInJoarkAsSystemUser(
        journalpostRequestAsFile: File,
    ): JournalpostResponse {
        val dataBufferFactory = DefaultDataBufferFactory()
        val dataBuffer = DataBufferUtils.read(journalpostRequestAsFile.toPath(), dataBufferFactory, 256 * 256)

        // Choose WebClient based on file size
        val fileSize = journalpostRequestAsFile.length()
        val webClient = if (fileSize > LARGE_FILE_THRESHOLD_BYTES) {
            logger.debug("Using large file WebClient ($LARGE_FILE_UPLOAD_TIMEOUT_SECONDS)s timeout) for file of size {} bytes", fileSize)
            joarkLargeFileWebClient
        } else {
            logger.debug("Using small file WebClient ($SMALL_FILE_UPLOAD_TIMEOUT_SECONDS)s timeout) for file of size {} bytes", fileSize)
            joarkSmallFileWebClient
        }

        val post = webClient.post()
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenUtil.getAppAccessTokenWithDokarkivScope()}")

        val startTime = System.currentTimeMillis()

        val journalpostResponse = post.contentType(MediaType.APPLICATION_JSON)
            .body<DataBuffer>(dataBuffer)
            .retrieve()
            .onStatus(HttpStatus.CONFLICT::equals) {
                logger.debug("Journalpost already exists. Returning journalpost id.")
                Mono.empty()
            }
            .onStatus(HttpStatus.CREATED::equals) {
                logger.debug("Journalpost successfully created in Joark")
                Mono.empty()
            }
            .bodyToMono<JournalpostResponse>()
            .block()
            ?: throw RuntimeException("Journalpost could not be created.")

        val durationMs = System.currentTimeMillis() - startTime
        val sizeInMB = String.format("%.2f", fileSize / (1024.0 * 1024.0))
        logger.debug(
            "POST journalpost call completed in {} ms ({} seconds). File size: {} bytes ({} MB)",
            durationMs,
            durationMs / 1000.0,
            fileSize,
            sizeInMB
        )

        // Log warning if request takes longer than expected based on file size
        val expectedMaxMs = when {
            fileSize < 500_000 -> 400L                      // 1 byte - 500KB: max 400ms
            fileSize < 1_000_000 -> 1_000L                  // 500KB - 1MB: max 1s
            fileSize < 2_000_000 -> 2_000L                  // 1MB - 2MB: max 2s
            fileSize < 5_000_000 -> 4_000L                  // 2MB - 5MB: max 4s
            fileSize < 15_000_000 -> 10_000L                // 5MB - 15MB: max 10s
            else -> 25_000L                                 // 15MB+: max 25s
        }
        if (durationMs > expectedMaxMs) {
            logger.warn(
                "Slow POST journalpost call: {} ms (expected max {} ms). File size: {} bytes ({} MB)",
                durationMs,
                expectedMaxMs,
                fileSize,
                sizeInMB
            )
        }

        logger.debug("Journalpost successfully created in Joark with id {}.", journalpostResponse.journalpostId)

        journalpostRequestAsFile.delete()

        return journalpostResponse
    }
}