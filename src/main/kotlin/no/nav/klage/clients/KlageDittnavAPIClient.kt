package no.nav.klage.clients

import no.nav.klage.domain.KlageApiJournalpost
import no.nav.klage.getLogger
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

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
        logger.debug("Registering journalpostid in klage-dittnav-api. KlageId: {}, journalpostId: {}", klageId, journalpostId)
        klageDittnavAPIWebClient.post()
                .uri("$klageId/journalpostid")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${azureADClient.oidcToken()}")
                .bodyValue(KlageApiJournalpost(journalpostId))
    }
}