package no.nav.klage.clients

import no.nav.klage.getLogger
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class FileClient(private val fileWebClient: WebClient) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun getAttachment(id: String): ByteArray {
        logger.debug("Fetching attachment with id {}", id)

        return this.fileWebClient.get()
            .uri { it.path("/attachment/{id}").build(id) }
            .retrieve()
            .bodyToMono<ByteArray>()
            .block() ?: throw RuntimeException("Attachment could not be fetched")
    }

    fun deleteAttachment(id: String) {
        logger.debug("Deleting attachment with id {}", id)

        val deletedInGCS = fileWebClient
                .delete()
                .uri("/attachment/$id")
                .retrieve()
                .bodyToMono<Boolean>()
                .block()

        if (deletedInGCS == true) {
            logger.debug("Attachment successfully deleted in file store.")
        } else {
            logger.warn("Could not successfully delete attachment in file store.")
        }
    }

    fun saveKlage(journalpostId: String, bytes: ByteArray) {
        logger.debug("Uploading klage to storage, with journalpost id {} ", journalpostId)

        val bodyBuilder = MultipartBodyBuilder()
        bodyBuilder.part("file", bytes).filename(journalpostId)
        val uploadSuccessful = fileWebClient
            .post()
            .uri("/klage/$journalpostId")
            .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
            .retrieve()
            .bodyToMono<Boolean>()
            .block()

        if (uploadSuccessful == true) {
            logger.debug("Klage was successfully uploaded in file store.")
        } else {
            logger.warn("Could not successfully upload klage to file store.")
        }
    }
}