package no.nav.klage.clients

import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpHeaders
import org.springframework.resilience.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.client.bodyToMono
import java.io.File
import java.nio.file.Files

@Component
class FileClient(
    private val fileWebClient: WebClient,
    private val tokenUtil: TokenUtil,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @Retryable
    fun getAttachment(id: String): File {
        logger.debug("Fetching attachment with id {}", id)

        val dataBufferFlux = this.fileWebClient.get()
            .uri { it.path("/attachment/{id}").build(id) }
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenUtil.getAppAccessTokenWithKlageFileApiScope()}")
            .retrieve()
            .bodyToFlux<DataBuffer>()

        val tempFile = Files.createTempFile(null, null)

        DataBufferUtils.write(dataBufferFlux, tempFile).block()
        return tempFile.toFile()
    }

    @Retryable
    fun deleteAttachment(id: String) {
        logger.debug("Deleting attachment with id {}", id)

        val deletedInGCS = fileWebClient
            .delete()
            .uri("/attachment/$id")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenUtil.getAppAccessTokenWithKlageFileApiScope()}")
            .retrieve()
            .bodyToMono<Boolean>()
            .block()

        if (deletedInGCS == true) {
            logger.debug("Attachment successfully deleted in file store.")
        } else {
            logger.warn("Could not successfully delete attachment in file store.")
        }
    }
}
