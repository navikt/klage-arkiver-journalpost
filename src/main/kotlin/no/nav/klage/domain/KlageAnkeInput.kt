package no.nav.klage.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDate

private const val BEHANDLINGSTEMA_LONNSKOMPENSASJON = "ab0438"
private const val BEHANDLINGSTEMA_TILBAKEBETALING_FORSKUDD_PAA_DAGPENGER = "ab0451"
private const val BEHANDLINGSTEMA_FERIEPENGER_AV_DAGPENGER = "ab0452"
private const val BEHANDLINGSTEMA_ENGANGSSTONAD = "ab0327"
private const val BEHANDLINGSTEMA_FORELDREPENGER = "ab0326"
private const val BEHANDLINGSTEMA_SVANGERSKAPSPENGER = "ab0126"

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
    val tema: String,
    val ytelse: String,
    val vedlegg: List<Vedlegg> = emptyList(),
    var fileContentAsBytes: ByteArray? = null,
    val userSaksnummer: String?,
    val internalSaksnummer: String?,
    val klageAnkeType: KlageAnkeType,
    //klage specific
    val userChoices: List<String>? = emptyList(),
    //anke specific
    val enhetsnummer: String?,
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
    fun getBehandlingstema(): String? {
        return when (klageAnkeType) {
            KlageAnkeType.KLAGE, KlageAnkeType.KLAGE_ETTERSENDELSE -> when {
                this.isLoennskompensasjon() -> BEHANDLINGSTEMA_LONNSKOMPENSASJON
                this.isTilbakebetalingAvForskuddPaaDagpenger() -> BEHANDLINGSTEMA_TILBAKEBETALING_FORSKUDD_PAA_DAGPENGER
                this.isFeriepengerAvDagpenger() -> BEHANDLINGSTEMA_FERIEPENGER_AV_DAGPENGER
                this.isForeldrepenger() -> BEHANDLINGSTEMA_FORELDREPENGER
                this.isEngangsstonad() -> BEHANDLINGSTEMA_ENGANGSSTONAD
                this.isSvangerskapspenger() -> BEHANDLINGSTEMA_SVANGERSKAPSPENGER
                else -> null
            }

            KlageAnkeType.ANKE, KlageAnkeType.ANKE_ETTERSENDELSE -> null
        }
    }
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