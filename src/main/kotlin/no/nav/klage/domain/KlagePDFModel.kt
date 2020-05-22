package no.nav.klage.domain

data class KlagePDFModel(
    val foedselsnummer: String,
    val navn: String,
    val adresse: String,
    val telefonnummer: String,
    val navenhet: String,
    val vedtaksdato: String,
    val kortRedegjoerelse: String,
    val begrunnelse: String,
    val navReferanse: String,
    val oversiktVedlegg: String,
    val sted: String,
    val dato: String,
    val brukersignatur: String,
    val navsignatur: String
)