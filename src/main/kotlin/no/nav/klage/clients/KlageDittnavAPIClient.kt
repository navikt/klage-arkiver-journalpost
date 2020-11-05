package no.nav.klage.clients

import no.nav.klage.domain.KlageApiJournalpost
import no.nav.klage.getLogger
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class KlageDittnavAPIClient(
        private val klageDittnavAPIWebClient: WebClient,
        private val azureADClient: AzureADClient
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun setJournalpostIdToKlage(klageId: Int, journalpostId: String) {
        logger.debug("Registering journalpost ID in klage-dittnav-api. KlageId: {}, journalpostId: {}", klageId, journalpostId)
        klageDittnavAPIWebClient.post()
                .uri("$klageId/journalpostid")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${azureADClient.klageDittnavApiOidcToken()}")
                .bodyValue(KlageApiJournalpost(journalpostId))
                .retrieve()
                .toBodilessEntity()
                .block() ?: throw RuntimeException("Unable to register journalpost ID in klage-dittnav-api.")
    }

    fun getJournalpostForKlageId(klageId: Int): JournalpostIdResponse {
        logger.debug("Getting journalpostId from klage-dittnav-api. KlageId: {}", klageId)
        return klageDittnavAPIWebClient.get()
            .uri("$klageId/journalpostid")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${azureADClient.klageDittnavApiOidcToken()}")
            .retrieve()
            .bodyToMono<JournalpostIdResponse>()
            .block() ?: throw RuntimeException("Unable to get journalpost ID from klage-dittnav-api.")
    }
}

data class JournalpostIdResponse(val journalpostId: String?)