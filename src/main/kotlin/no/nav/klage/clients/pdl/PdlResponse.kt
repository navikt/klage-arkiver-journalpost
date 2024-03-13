package no.nav.klage.clients.pdl

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class HentPersonResponse(val data: PdlPersonDataWrapper?, val errors: List<PdlError>? = null)

data class PdlPersonDataWrapper(val hentPerson: PdlPerson?)

data class PdlPerson(
    val adressebeskyttelse: List<Adressebeskyttelse>,
) {
    data class Adressebeskyttelse(val gradering: GraderingType) {
        enum class GraderingType { STRENGT_FORTROLIG_UTLAND, STRENGT_FORTROLIG, FORTROLIG, UGRADERT }
    }
}

data class PdlError(
    val message: String,
    val locations: List<PdlErrorLocation>,
    val path: List<String>?,
    val extensions: PdlErrorExtension
)

data class PdlErrorLocation(
    val line: Int?,
    val column: Int?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlErrorExtension(
    val code: String?,
    val classification: String?,
    val warnings: List<String>?,
)