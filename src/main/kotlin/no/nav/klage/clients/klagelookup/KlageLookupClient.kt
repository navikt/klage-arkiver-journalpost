package no.nav.klage.clients.klagelookup

import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import no.nav.klage.util.logErrorResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.resilience.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class KlageLookupClient(
    private val klageLookupWebClient: WebClient,
    private val tokenUtil: TokenUtil,
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @Retryable
    fun getPerson(fnr: String): PersonResponse =
        runWithTimingAndLogging {
            klageLookupWebClient
                .post()
                .uri("/person")
                .bodyValue(
                    GetPersonRequest(
                        fnr = fnr,
                    ),
                ).header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer ${tokenUtil.getAppAccessTokenWithKlageLookupScope()}",
                ).retrieve()
                .onStatus(HttpStatusCode::isError) { response ->
                    logErrorResponse(
                        response = response,
                        functionName = ::getPerson.name,
                        classLogger = logger,
                    )
                }.bodyToMono<PersonResponse>()
                .block() ?: throw RuntimeException("Could not get person. Response was null.")
        }

    private fun <T> runWithTimingAndLogging(block: () -> T): T {
        val start = System.currentTimeMillis()
        try {
            return block.invoke()
        } finally {
            val end = System.currentTimeMillis()
            logger.debug("Time it took to call klage-lookup: ${end - start} millis")
        }
    }
}
