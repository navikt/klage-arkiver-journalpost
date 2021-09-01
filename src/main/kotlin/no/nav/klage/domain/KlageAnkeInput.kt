package no.nav.klage.domain

import java.time.LocalDate

private const val BEHANDLINGSTEMA_LONNSKOMPENSASJON = "ab0438"
private const val BEHANDLINGSTEMA_TILBAKEBETALING_FORSKUDD_PAA_DAGPENGER = "ab0451"
private const val BEHANDLINGSTEMA_FERIEPENGER_AV_DAGPENGER = "ab0452"
private const val BEHANDLINGSTEMA_ENGANGSSTONAD = "ab0327"
private const val BEHANDLINGSTEMA_FORELDREPENGER = "ab0326"
private const val BEHANDLINGSTEMA_SVANGERSKAPSPENGER = "ab0126"

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
    val klageAnkeType: KlageAnkeType = no.nav.klage.domain.KlageAnkeType.KLAGE,
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

    fun isForeldrepenger(): Boolean {
        return tema == "FOR" && ytelse == "Foreldrepenger"
    }

    fun isEngangsstonad(): Boolean {
        return tema == "FOR" && ytelse == "Engangsstonad"
    }

    fun isSvangerskapspenger(): Boolean {
        return tema == "FOR" && ytelse == "Svangerskapspenger"
    }

    fun isDagpengerVariant(): Boolean {
        return (isLoennskompensasjon() ||
                isTilbakebetalingAvForskuddPaaDagpenger() ||
                isFeriepengerAvDagpenger())
    }

    fun getBehandlingstema(): String? {
        return if (isKlage()) {
            when {
                this.isLoennskompensasjon() -> BEHANDLINGSTEMA_LONNSKOMPENSASJON
                this.isTilbakebetalingAvForskuddPaaDagpenger() -> BEHANDLINGSTEMA_TILBAKEBETALING_FORSKUDD_PAA_DAGPENGER
                this.isFeriepengerAvDagpenger() -> BEHANDLINGSTEMA_FERIEPENGER_AV_DAGPENGER
                this.isForeldrepenger() -> BEHANDLINGSTEMA_FORELDREPENGER
                this.isEngangsstonad() -> BEHANDLINGSTEMA_ENGANGSSTONAD
                this.isSvangerskapspenger() -> BEHANDLINGSTEMA_SVANGERSKAPSPENGER
                else -> null
            }
        } else null
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