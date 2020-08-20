package no.nav.klage.clients

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.klage.getLogger
import no.nav.klage.utils.Kibana
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

enum class Severity(val emoji: String) {
    ERROR(":scream:"),
    WARN(":thinking_face"),
    INFO(":information_source:")
}

fun SlackClient.post(message: String, severity: Severity = Severity.INFO) {
    this.postMessage(message, severity)
}

fun SlackClient.post(klageId: Int, message: String, severity: Severity = Severity.INFO) {
    val text = String.format("<%s|%s> %s",
            Kibana.createUrl(klageId.toString()),
            klageId.toString(),
            message
    )

    this.postMessage(text, severity)
}

class SlackClient(
        private val url: String,
        private val channel: String,
        private val clusterName: String
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    fun postMessage(text: String, severity: Severity) {
        url.post(objectMapper.writeValueAsString(mutableMapOf<String, Any>(
                "channel" to channel,
                "username" to "klage-arkiver-journalpost ($clusterName)",
                "text" to text,
                "icon_emoji" to severity.emoji
        )))
    }

    private fun String.post(jsonPayload: String): String? {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(this).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 5000
                readTimeout = 5000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")

                outputStream.use { it.bufferedWriter(Charsets.UTF_8).apply { write(jsonPayload); flush() } }
            }

            val responseCode = connection.responseCode

            if (connection.responseCode !in 200..299) {
                logger.warn("response from slack: code=$responseCode")
                return null
            }

            val responseBody = connection.inputStream.readText()
            logger.debug("response from slack: code=$responseCode")

            return responseBody
        } catch (err: SocketTimeoutException) {
            logger.warn("timeout waiting for reply", err)
        } catch (err: IOException) {
            logger.error("feil ved posting til slack: {}", err.message, err)
        } finally {
            connection?.disconnect()
        }

        return null
    }

    private fun InputStream.readText() = use { it.bufferedReader().readText() }
}
