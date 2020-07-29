package no.nav.klage.clients

import no.nav.klage.getLogger
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class KlageDittnavAPIClient(private val klageDittnavAPIWebClient: WebClient) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun setJournalpostIdToKlage(klageId: Int, journalpostId: String) {
        logger.debug("TODO")

        //TODO
//        klageDittnavAPIWebClient.post()
    }
}