package no.nav.klage.clients.klagelookup

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class PersonResponse(
    val foedselsnr: String,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val sammensattNavn: String,
    val kjoenn: String?,
    val doed: LocalDate?,
    val strengtFortrolig: Boolean,
    val strengtFortroligUtland: Boolean,
    val fortrolig: Boolean,
    val egenAnsatt: Boolean,
    val vergemaalEllerFremtidsfullmakt: Boolean,
    val sikkerhetstiltak: SikkerhetstiltakResponse?,
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class SikkerhetstiltakResponse(
        val tiltakstype: String,
        val beskrivelse: String,
        val gyldigFraOgMed: LocalDate,
        val gyldigTilOgMed: LocalDate,
    )
}
