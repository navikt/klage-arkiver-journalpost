package no.nav.klage.clients

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.klage.getLogger
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.LocalDateTime

@Component
class StsClient(private val stsWebClient: WebClient) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private var cachedOidcToken: Token? = null
    }

    fun oidcToken(): String {
        if (cachedOidcToken.shouldBeRenewed()) {
            logger.debug("Getting token from STS")
            cachedOidcToken = stsWebClient.get()
                .uri { uriBuilder ->
                    uriBuilder
                        .queryParam("grant_type", "client_credentials")
                        .queryParam("scope", "openid")
                        .build()
                }
                .retrieve()
                .bodyToMono<Token>()
                .block()
        }

        return cachedOidcToken!!.token
    }

    private fun Token?.shouldBeRenewed(): Boolean = this?.hasExpired() ?: true

    data class Token(
        @JsonProperty(value = "access_token", required = true)
        val token: String,
        @JsonProperty(value = "token_type", required = true)
        val type: String,
        @JsonProperty(value = "expires_in", required = true)
        val expiresIn: Int
    ) {
        private val expirationTime: LocalDateTime = LocalDateTime.now().plusSeconds(expiresIn - 20L)

        fun hasExpired(): Boolean = expirationTime.isBefore(LocalDateTime.now())
    }
}
