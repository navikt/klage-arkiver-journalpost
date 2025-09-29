package no.nav.klage.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class KlageAnkeInput(
    val id: String,
    val identifikasjonsnummer: String,
    val fornavn: String,
    val mellomnavn: String,
    val etternavn: String,
    val vedtak: String,
    val dato: LocalDate,
    val begrunnelse: String,
    val ytelse: String,
    val vedlegg: List<Vedlegg> = emptyList(),
    var fileContentAsBytes: ByteArray? = null,
    val userSaksnummer: String?,
    val internalSaksnummer: String?,
    val sak: Sak?,
    val klageAnkeType: KlageAnkeType,
    //Only relevant for ettersendelse klage
    val ettersendelseTilKa: Boolean?,
    val innsendingsYtelseId: String,
) {
    data class Sak(
        val sakstype: String,
        var fagsaksystem: String,
        var fagsakid: String,
    )
}

enum class KlageAnkeType {
    KLAGE, ANKE, KLAGE_ETTERSENDELSE, ANKE_ETTERSENDELSE
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Vedlegg(
    val tittel: String,
    val ref: String,
    var fileContentAsBytes: ByteArray? = null,
)

fun String.toKlageAnkeInput(): KlageAnkeInput = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .readValue(this, KlageAnkeInput::class.java)