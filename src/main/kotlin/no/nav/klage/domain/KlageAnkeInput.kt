package no.nav.klage.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.klage.kodeverk.innsendingsytelse.Innsendingsytelse
import java.io.File
import java.time.LocalDate

private const val BEHANDLINGSTEMA_LONNSKOMPENSASJON = "ab0438"
private const val BEHANDLINGSTEMA_TILBAKEBETALING_FORSKUDD_PAA_DAGPENGER = "ab0451"
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
    val vedlegg: List<MellomlagretDokument> = emptyList(),
    var hoveddokument: MellomlagretDokument? = null,
    val userSaksnummer: String?,
    val internalSaksnummer: String?,
    val klageAnkeType: KlageAnkeType,
    //klage specific
    val userChoices: List<String>? = emptyList(),
    //Only relevant for ettersendelse klage
    val ettersendelseTilKa: Boolean?,
    val innsendingsYtelseId: String?,
) {
    @JsonIgnore
    fun isLoennskompensasjon(): Boolean {
        return if (innsendingsYtelseId.isNullOrBlank()) {
            tema == "DAG" && ytelse == "Lønnskompensasjon for permitterte"
        } else {
            Innsendingsytelse.of(innsendingsYtelseId) == Innsendingsytelse.LONNSKOMPENSASJON
        }
    }

    @JsonIgnore
    fun isTilbakebetalingAvForskuddPaaDagpenger(): Boolean {
        return if (innsendingsYtelseId.isNullOrBlank()) {
            return tema == "DAG" && ytelse == "Tilbakebetaling av forskudd på dagpenger"
        } else {
            Innsendingsytelse.of(innsendingsYtelseId) == Innsendingsytelse.DAGPENGER_TILBAKEBETALING_FORSKUDD
        }
    }

    @JsonIgnore
    fun isForeldrepenger(): Boolean {
        return if (innsendingsYtelseId.isNullOrBlank()) {
            return tema == "FOR" && ytelse == "Foreldrepenger"
        } else {
            Innsendingsytelse.of(innsendingsYtelseId) == Innsendingsytelse.FORELDREPENGER
        }
    }

    @JsonIgnore
    fun isEngangsstonad(): Boolean {
        return if (innsendingsYtelseId.isNullOrBlank()) {
            return tema == "FOR" && ytelse == "Engangsstønad"
        } else {
            Innsendingsytelse.of(innsendingsYtelseId) == Innsendingsytelse.ENGANGSSTONAD
        }
    }

    @JsonIgnore
    fun isSvangerskapspenger(): Boolean {
        return if (innsendingsYtelseId.isNullOrBlank()) {
            return tema == "FOR" && ytelse == "Svangerskapspenger"
        } else {
            Innsendingsytelse.of(innsendingsYtelseId) == Innsendingsytelse.SVANGERSKAPSPENGER
        }
    }

    @JsonIgnore
    fun getBehandlingstema(): String? {
        return when (klageAnkeType) {
            KlageAnkeType.KLAGE, KlageAnkeType.KLAGE_ETTERSENDELSE -> when {
                this.isLoennskompensasjon() -> BEHANDLINGSTEMA_LONNSKOMPENSASJON
                this.isTilbakebetalingAvForskuddPaaDagpenger() -> BEHANDLINGSTEMA_TILBAKEBETALING_FORSKUDD_PAA_DAGPENGER
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
data class MellomlagretDokument(
    val tittel: String,
    val ref: String?,
    var file: File,
)

fun String.toKlageAnkeInput(): KlageAnkeInput = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .readValue(this, KlageAnkeInput::class.java)