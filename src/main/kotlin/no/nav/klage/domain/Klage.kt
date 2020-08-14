package no.nav.klage.domain

import java.time.LocalDate

data class Klage(
    val id: Int,
    val identifikasjonstype: String,
    val identifikasjonsnummer: String,
    val klageInstans: Boolean,
    val trygderetten: Boolean,
    val fornavn: String,
    val mellomnavn: String,
    val etternavn: String,
    val adresse: String,
    val telefon: String,
    val vedtak: String,
    val saksnummer: String?,
    val dato: LocalDate,
    val begrunnelse: String,
    val tema: String,
    val ytelse: String,
    val vedlegg: List<Vedlegg> = emptyList(),
    var fileContentAsBytes: ByteArray? = null
)

data class Vedlegg(
    val id: Int,
    val tittel: String,
    val ref: String,
    val klageId: Int,
    val contentType: String,
    var fileContentAsBytes: ByteArray? = null,
    val sizeInBytes: Int
)