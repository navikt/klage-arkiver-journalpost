package no.nav.klage.domain

data class Klage(
    val id: Int,
    val klageInstans: Boolean,
    val trygderetten: Boolean,
    val navn: String,
    val adresse: String,
    val telefon: String,
    val navenhet: String,
    val vedtaksdato: String,
    val navReferanse: String,
    val kortRedegjoerelse: String,
    val stedDato: String,
    val oversiktVedlegg: String,
    val begrunnelse: String,
    val foedselsnummer: String,
    var attachments: Array<Attachment> = emptyArray()
)

data class Attachment(
    val id: String,
    val name: String
)
