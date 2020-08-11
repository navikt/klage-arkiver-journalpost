package no.nav.klage.clients

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.klage.domain.OidcToken
import no.nav.klage.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class AzureADClient(
        private val azureADWebClient: WebClient
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private var cachedOidcToken: OidcToken? = null
        private var cachedOidcDiscovery: OidcDiscovery? = null
    }

    @Value("\${AZURE_APP_CLIENT_ID}")
    private lateinit var clientId: String

    @Value("\${AZURE_APP_CLIENT_SECRET}")
    private lateinit var clientSecret: String

    @Value("\${AZURE_APP_WELL_KNOWN_URL}")
    private lateinit var discoveryUrl: String

    @Value("\${KLAGE_DITTNAV_API_CLIENT_ID}")
    private lateinit var klageDittnavApiClientId: String

    private fun oidcDiscovery(): OidcDiscovery {
        if (cachedOidcDiscovery == null) {
            logger.debug("getting info from oidcDiscovery")
            cachedOidcDiscovery = azureADWebClient.get()
                    .uri(discoveryUrl)
                    .retrieve()
                    .bodyToMono<OidcDiscovery>()
                    .block()

            logger.debug("Retrieved endpoint: " + cachedOidcDiscovery!!.token_endpoint)
        }

        return cachedOidcDiscovery!!
    }

    fun oidcToken(): String {
        if (cachedOidcToken.shouldBeRenewed()) {
            val map = LinkedMultiValueMap<String, String>()

            map.add("client_id", clientId)
            map.add("client_secret", clientSecret)
            map.add("grant_type", "client_credentials")
            map.add("scope", "api://${klageDittnavApiClientId}/.default")

            logger.debug("Getting access token from OIDC")

            cachedOidcToken = azureADWebClient.post()
                    .uri(oidcDiscovery().token_endpoint)
                    .bodyValue(map)
                    .retrieve()
                    .bodyToMono<OidcToken>()
                    .block()
        }

        return cachedOidcToken!!.token
    }

    private fun OidcToken?.shouldBeRenewed(): Boolean = this?.hasExpired() ?: true

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class OidcDiscovery(val token_endpoint: String, val jwks_uri: String, val issuer: String)
}