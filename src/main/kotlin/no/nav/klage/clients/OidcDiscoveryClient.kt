package no.nav.klage.clients

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.klage.getLogger
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class OidcDiscoveryClient(private val oidcDiscoveryWebClient: WebClient) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private var cachedOidcDiscovery: OidcDiscovery? = null
    }

    fun oidcDiscovery(): OidcDiscovery {
        if (cachedOidcDiscovery == null) {
            logger.debug("getting info from oidcDiscovery")
            cachedOidcDiscovery = oidcDiscoveryWebClient.get()
                    .retrieve()
                    .bodyToMono<OidcDiscovery>()
                    .block()
        }
        logger.debug("Retrieved endpoint: " + cachedOidcDiscovery!!.token_endpoint)
        return cachedOidcDiscovery!!
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class OidcDiscovery(val token_endpoint: String, val jwks_uri: String, val issuer: String)
}