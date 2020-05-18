package no.nav.klage.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient
import java.util.*

@Configuration
class StsClientConfiguration {

    @Value("\${SECURITY_TOKEN_SERVICE_REST_URL}")
    private lateinit var stsUrl: String

    @Value("\${SERVICE_USER_USERNAME}")
    private lateinit var username: String

    @Value("\${SERVICE_USER_PASSWORD}")
    private lateinit var password: String

    @Value("\${STS_APIKEY}")
    private lateinit var apiKey: String

    @Bean
    fun stsWebClient(): WebClient {
        return WebClient
            .builder()
            .baseUrl("$stsUrl/rest/v1/sts/token")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic ${credentials()}")
            .defaultHeader("x-nav-apiKey", apiKey)
            .build()
    }

    private fun credentials() =
        Base64.getEncoder().encodeToString("${username}:${password}".toByteArray(Charsets.UTF_8))
}