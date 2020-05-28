package no.nav.klage.clients

import no.nav.klage.getLogger
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class AttachmentClient(private val attachmentWebClient: WebClient) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun getAttachment(id: String): ByteArray {
        logger.debug("Fetching attachment with id {}", id)

        return this.attachmentWebClient.get()
            .uri { it.path("/$id").build(id) }
            .retrieve()
            .bodyToMono<ByteArray>()
            .block() ?: throw RuntimeException("Attachment could not be fetched")
    }

    fun deleteAttachment(id: String) {
        logger.debug("Deleting attachment with id {}", id)

        attachmentWebClient.delete()
            .uri { it.path("/$id").build(id) }
            .retrieve()
    }
}