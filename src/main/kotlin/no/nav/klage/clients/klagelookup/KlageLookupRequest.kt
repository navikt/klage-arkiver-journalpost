package no.nav.klage.clients.klagelookup

import no.nav.klage.kodeverk.Fagsystem
import no.nav.klage.kodeverk.ytelse.Ytelse

data class GetPersonRequest(
    val fnr: String,
)

data class Sak(
    val sakId: String,
    val ytelse: Ytelse,
    val fagsystem: Fagsystem,
)
