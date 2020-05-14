package no.nav.klage.clients

import com.google.cloud.storage.StorageOptions
import no.nav.klage.domain.Attachment
import org.springframework.stereotype.Component

@Component
class GCPStorageClient {
    fun getAttachments(attachments: Array<Attachment>) {
        val storage = StorageOptions.newBuilder()
            .setProjectId("xxx")
            .build()
            .service

        val bucket = "yyy"

        attachments.forEach {
            val path = "zzz"
            val content = storage.get(bucket, path)
        }
    }
}