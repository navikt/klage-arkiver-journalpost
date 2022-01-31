package no.nav.klage.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
    //deprecated, only used to parse old kafka entries
    val saksnummer: String? = null,
    val userChoices: List<String>? = emptyList(),
    val userSaksnummer: String?,
    val internalSaksnummer: String?,
    val fullmektigNavn: String?,
    val fullmektigFnr: String?,
    val klageAnkeType: KlageAnkeType = no.nav.klage.domain.KlageAnkeType.KLAGE,
    val previousUtfall: String?
) {
    @JsonIgnore
    fun isLoennskompensasjon(): Boolean {
        return tema == "DAG" && ytelse == "Lønnskompensasjon for permitterte"
    }
    @JsonIgnore
    fun isTilbakebetalingAvForskuddPaaDagpenger(): Boolean {
        return tema == "DAG" && ytelse == "Tilbakebetaling av forskudd på dagpenger"
    }
    @JsonIgnore
    fun isFeriepengerAvDagpenger(): Boolean {
        return tema == "DAG" && ytelse == "Feriepenger av dagpenger"
    }
    @JsonIgnore
    fun isForeldrepenger(): Boolean {
        return tema == "FOR" && ytelse == "Foreldrepenger"
    }
    @JsonIgnore
    fun isEngangsstonad(): Boolean {
        return tema == "FOR" && ytelse == "Engangsstønad"
    }
    @JsonIgnore
    fun isSvangerskapspenger(): Boolean {
        return tema == "FOR" && ytelse == "Svangerskapspenger"
    }
    @JsonIgnore
    fun isDagpengerVariant(): Boolean {
        return (isLoennskompensasjon() ||
                isTilbakebetalingAvForskuddPaaDagpenger() ||
                isFeriepengerAvDagpenger())
    }
    @JsonIgnore
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
    @JsonIgnore
    fun isKlage(): Boolean {
        return klageAnkeType == KlageAnkeType.KLAGE
    }

    val deprecatedFields = listOf(saksnummer)

    fun containsDeprecatedFields(): Boolean {
        deprecatedFields.forEach { if (it != null) return true }
        return false
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

fun String.toKlage(): KlageAnkeInput = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .readValue(this, KlageAnkeInput::class.java)