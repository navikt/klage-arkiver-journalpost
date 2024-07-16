package no.nav.klage.clients

import no.nav.klage.getLogger
import no.nav.klage.getSecureLogger
import no.nav.klage.logErrorResponse
import no.nav.klage.util.TokenUtil
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
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
        private val secureLogger = getSecureLogger()
    }

    fun getAttachment(id: String): File {
        logger.debug("Fetching attachment with id {}", id)

        val dataBufferFlux = fileWebClient.get()
            .uri { it.path("/attachment/{id}/outputstream").build(id) }
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenUtil.getAppAccessTokenWithKlageFileApiScope()}")
            .retrieve()
            .onStatus(HttpStatusCode::isError) { response ->
                logErrorResponse(response, ::getAttachment.name, secureLogger)
            }
            .bodyToFlux(DataBuffer::class.java)

        val tempFile = Files.createTempFile(null, null)

        DataBufferUtils.write(dataBufferFlux, tempFile).block()
        return tempFile.toFile()
    }

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
