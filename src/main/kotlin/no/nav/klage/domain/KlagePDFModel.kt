package no.nav.klage.domain

data class KlagePDFModel(
    val foedselsnummer: String,
    val fornavn: String,
    val mellomnavn: String,
    val etternavn: String,
    val adresse: String,
    val telefonnummer: String,
    val vedtak: String,
    val begrunnelse: String,
    val saksnummer: String,
    val oversiktVedlegg: String,
    val dato: String,
    val ytelse: String
)