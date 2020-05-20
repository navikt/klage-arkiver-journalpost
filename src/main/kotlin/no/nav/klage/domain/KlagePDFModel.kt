package no.nav.klage.domain

data class KlagePDFModel(
    val foedselsnummer: String,
    val fornavn: String,
    val etternavn: String,
    val adresse: String,
    val postnummer: String,
    val poststed: String,
    val telefonnummer: String,
    val NAVenhet: String,
    val vedtaksdato: String,
    val shortWhyShouldChange: String,
    val longWhyShouldChange: String,
    val NAVReference: String,
    val attachments: String,
    val place: String,
    val date: String,
    val userSignature: String,
    val NAVSignature: String
)