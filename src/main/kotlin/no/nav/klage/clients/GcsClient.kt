package no.nav.klage.clients

import com.google.cloud.storage.Storage
import no.nav.klage.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class GcsClient(private val gcsStorage: Storage) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @Value("\${GCS_BUCKET}")
    private lateinit var bucket: String

    fun getAttachment(path: String): ByteArray {
        logger.debug("Fetching attachments from GCP. Bucket: {}, path: {}", bucket, path)
        return gcsStorage.get(bucket, path).getContent()
    }
}