package no.nav.klage.clients

import no.nav.klage.domain.KlageApiJournalpost
import no.nav.klage.getLogger
import no.nav.klage.util.TokenUtil
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.util.*

@Component
class KlageDittnavAPIClient(
    private val klageDittnavAPIWebClient: WebClient,
    private val tokenUtil: TokenUtil,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun setJournalpostIdInKlanke(klankeId: String, journalpostId: String) {
        logger.debug("Registering journalpost ID for klanke in klage-dittnav-api. KlankeId ref: {}, journalpostId: {}", klankeId, journalpostId)
        klageDittnavAPIWebClient.post()
            .uri("klanker/$klankeId/journalpostid")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenUtil.getAppAccessTokenWithKlageDittnavApiScope()}")
            .bodyValue(KlageApiJournalpost(journalpostId))
            .retrieve()
            .toBodilessEntity()
            .block() ?: throw RuntimeException("Unable to register journalpost ID for klanke in klage-dittnav-api.")
    }

    fun getJournalpostForKlankeId(ankeId: String): JournalpostIdResponse {
        logger.debug("Getting journalpostId for klanke from klage-dittnav-api. KlankeId: {}", ankeId)
        return klageDittnavAPIWebClient.get()
            .uri("klanker/$ankeId/journalpostid")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenUtil.getAppAccessTokenWithKlageDittnavApiScope()}")
            .retrieve()
            .bodyToMono<JournalpostIdResponse>()
            .block() ?: throw RuntimeException("Unable to get journalpost ID for klanke from klage-dittnav-api.")
    }
}

data class JournalpostIdResponse(val journalpostId: String?)