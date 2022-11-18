package no.nav.klage.util

import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import org.springframework.stereotype.Service

@Service
class TokenUtil(
    private val clientConfigurationProperties: ClientConfigurationProperties,
    private val oAuth2AccessTokenService: OAuth2AccessTokenService,
) {
    fun getAppAccessTokenWithDokarkivScope(): String {
        val clientProperties = clientConfigurationProperties.registration["dokarkiv-maskintilmaskin"]
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
        return response.accessToken
    }

    fun getAppAccessTokenWithKlageFileApiScope(): String {
        val clientProperties = clientConfigurationProperties.registration["klage-file-api-maskintilmaskin"]
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
        return response.accessToken
    }

    fun getAppAccessTokenWithKlageDittnavApiScope(): String {
        val clientProperties = clientConfigurationProperties.registration["klage-dittnav-api-maskintilmaskin"]
        val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
        return response.accessToken
    }
}