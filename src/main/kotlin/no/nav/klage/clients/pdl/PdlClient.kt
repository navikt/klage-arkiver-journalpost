package no.nav.klage.clients.pdl

import no.nav.klage.getLogger
import no.nav.klage.getTeamLogger
import no.nav.klage.util.TokenUtil
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.lang.System.currentTimeMillis

@Component
class PdlClient(
    private val pdlWebClient: WebClient,
    private val tokenUtil: TokenUtil
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val teamLogger = getTeamLogger()
    }

    fun <T> runWithTiming(block: () -> T): T {
        val start = currentTimeMillis()
        try {
            return block.invoke()
        } finally {
            val end = currentTimeMillis()
            logger.debug("Time it took to call pdl: ${end - start} millis")
        }
    }

    @Retryable
    fun getPersonAdresseBeskyttelse(fnr: String): HentPersonResponse {
        return runWithTiming {
            val stsSystembrukerToken = tokenUtil.getAppAccessTokenWithPdlScope()
            pdlWebClient.post()
                .header(HttpHeaders.AUTHORIZATION, "Bearer $stsSystembrukerToken")
                .bodyValue(hentPersonQuery(fnr))
                .retrieve()
                .onStatus(HttpStatusCode::isError) { response ->
                    response.bodyToMono(String::class.java).map {
                        val errorString = "Got ${response.statusCode()} when requesting getPersonAdresseBeskyttelse"
                        teamLogger.error("$errorString - response body: {}", it)
                        RuntimeException(errorString)
                    }
                }
                .bodyToMono<HentPersonResponse>()
                .block() ?: throw RuntimeException("Person not found")
        }
    }
}