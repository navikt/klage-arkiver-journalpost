package no.nav.klage.domain

data class KlagePDFModel(
    val foedselsnummer: String,
    val fornavn: String,
    val mellomnavn: String,
    val etternavn: String,
    val adresse: String,
    val telefonnummer: String,
    val navEnhet: String,
    val vedtaksdato: String,
    val begrunnelse: String,
    val navReferanse: String,
    val oversiktVedlegg: String,
    val dato: String,
    val ytelse: String
)