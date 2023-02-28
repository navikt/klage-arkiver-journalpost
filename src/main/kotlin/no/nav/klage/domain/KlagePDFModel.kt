package no.nav.klage.domain

data class KlagePDFModel(
    val foedselsnummer: String,
    val fornavn: String,
    val mellomnavn: String,
    val etternavn: String,
    val vedtak: String,
    val begrunnelse: String,
    val saksnummer: String,
    val oversiktVedlegg: String,
    val dato: String,
    val ytelse: String,
    val userChoices: List<String>? = emptyList(),
)