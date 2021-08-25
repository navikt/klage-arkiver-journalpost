package no.nav.klage.domain

import java.time.LocalDate

data class KlageAnkeInput(
    val id: Int,
    val identifikasjonstype: String,
    val identifikasjonsnummer: String,
    val klageInstans: Boolean?,
    val trygderetten: Boolean?,
    val fornavn: String,
    val mellomnavn: String,
    val etternavn: String,
    val adresse: String,
    val telefon: String,
    val vedtak: String,
    val dato: LocalDate,
    val begrunnelse: String,
    val tema: String,
    val ytelse: String,
    val vedlegg: List<Vedlegg> = emptyList(),
    var fileContentAsBytes: ByteArray? = null,
    val userChoices: List<String>? = emptyList(),
    val userSaksnummer: String?,
    val internalSaksnummer: String?,
    val fullmektigNavn: String?,
    val fullmektigFnr: String?,
    val klageAnkeType: KlageAnkeType? = no.nav.klage.domain.KlageAnkeType.KLAGE,
    val previousUtfall: String?
) {

    fun isLoennskompensasjon(): Boolean {
        return tema == "DAG" && ytelse == "Lønnskompensasjon for permitterte"
    }

    fun isTilbakebetalingAvForskuddPaaDagpenger(): Boolean {
        return tema == "DAG" && ytelse == "Tilbakebetaling av forskudd på dagpenger"
    }

    fun isFeriepengerAvDagpenger(): Boolean {
        return tema == "DAG" && ytelse == "Feriepenger av dagpenger"
    }

    fun isDagpengerVariant(): Boolean {
        return (isLoennskompensasjon() ||
                isTilbakebetalingAvForskuddPaaDagpenger() ||
                isFeriepengerAvDagpenger())
    }

    fun isKlage(): Boolean {
        return klageAnkeType == KlageAnkeType.KLAGE
    }
}

enum class KlageAnkeType {
    KLAGE, ANKE
}

data class Vedlegg(
    val id: Int,
    val tittel: String,
    val ref: String,
    val klageId: Int,
    val contentType: String,
    var fileContentAsBytes: ByteArray? = null,
    val sizeInBytes: Int
)