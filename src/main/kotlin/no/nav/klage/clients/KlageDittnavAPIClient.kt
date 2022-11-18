package no.nav.klage.clients

import no.nav.klage.domain.KlageApiJournalpost
import no.nav.klage.getLogger
import no.nav.klage.util.TokenUtil
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class KlageDittnavAPIClient(
    private val klageDittnavAPIWebClient: WebClient,
    private val tokenUtil: TokenUtil,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun setJournalpostIdToKlage(klageId: Int, journalpostId: String) {
        logger.debug("Registering journalpost ID for klage in klage-dittnav-api. KlageId: {}, journalpostId: {}", klageId, journalpostId)
        klageDittnavAPIWebClient.post()
                .uri("klager/$klageId/journalpostid")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenUtil.getAppAccessTokenWithKlageDittnavApiScope()}")
                .bodyValue(KlageApiJournalpost(journalpostId))
                .retrieve()
                .toBodilessEntity()
                .block() ?: throw RuntimeException("Unable to register journalpost ID in klage-dittnav-api.")
    }

    fun getJournalpostForKlageId(klageId: Int): JournalpostIdResponse {
        logger.debug("Getting journalpostId for klage from klage-dittnav-api. KlageId: {}", klageId)
        return klageDittnavAPIWebClient.get()
            .uri("klager/$klageId/journalpostid")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenUtil.getAppAccessTokenWithKlageDittnavApiScope()}")
            .retrieve()
            .bodyToMono<JournalpostIdResponse>()
            .block() ?: throw RuntimeException("Unable to get journalpost ID from klage-dittnav-api.")
    }

    fun setJournalpostIdToAnke(internalSaksnummer: String, journalpostId: String) {
        logger.debug("Registering journalpost ID for anke in klage-dittnav-api. Internal ref: {}, journalpostId: {}", internalSaksnummer, journalpostId)
        klageDittnavAPIWebClient.post()
            .uri("anker/$internalSaksnummer/journalpostid")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenUtil.getAppAccessTokenWithKlageDittnavApiScope()}")
            .bodyValue(KlageApiJournalpost(journalpostId))
            .retrieve()
            .toBodilessEntity()
            .block() ?: throw RuntimeException("Unable to register journalpost ID for anke in klage-dittnav-api.")
    }

    fun getJournalpostForAnkeInternalSaksnummer(internalSaksnummer: String): JournalpostIdResponse {
        logger.debug("Getting journalpostId for anke from klage-dittnav-api. Internal ref: {}", internalSaksnummer)
        return klageDittnavAPIWebClient.get()
            .uri("anker/$internalSaksnummer/journalpostid")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenUtil.getAppAccessTokenWithKlageDittnavApiScope()}")
            .retrieve()
            .bodyToMono<JournalpostIdResponse>()
            .block() ?: throw RuntimeException("Unable to get journalpost ID for anke from klage-dittnav-api.")
    }
}

data class JournalpostIdResponse(val journalpostId: String?)