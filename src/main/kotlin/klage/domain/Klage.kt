package klage.domain

import java.time.LocalDate

data class Klage(
    val id: Int,
    val klageInstans: Boolean,
    val trygderetten: Boolean,
    val navn: String,
    val adresse: String,
    val telefon: String,
    val navenhet: String,
    val vedtaksdato: LocalDate,
    val navReferanse: String,
    val kortRedegjoerelse: String,
    val sted: String,
    val dato: LocalDate,
    val oversiktVedlegg: String,
    val begrunnelse: String,
    val foedselsnummer: String,
    val tema: String,
    val vedlegg: List<Vedlegg> = emptyList(),
    var fileContentAsBytes: ByteArray? = null
)

data class Vedlegg(
    val id: Int,
    val tittel: String,
    val gcsRef: String,
    val klageId: Int,
    var fileContentAsBytes: ByteArray? = null
)