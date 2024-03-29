package no.nav.klage.domain

data class Journalpost(
    val journalposttype: String = "INNGAAENDE",
    val tema: String,
    val behandlingstema: String? = null,
    val kanal: String = "NAV_NO",
    val tittel: String,
    val avsenderMottaker: AvsenderMottaker,
    val bruker: Bruker,
    val sak: Sak? = null,
    val dokumenter: List<Dokument> = mutableListOf(),
    val tilleggsopplysninger: List<Tilleggsopplysning> = mutableListOf(),
    val eksternReferanseId: String,
    val journalfoerendeEnhet: String?,
)

data class Tilleggsopplysning(val nokkel: String, val verdi: String)

data class Dokument(
    val tittel: String,
    val brevkode: String? = null,
    val dokumentVarianter: List<DokumentVariant> = mutableListOf()
)

data class DokumentVariant(
    val filtype: String,
    val fysiskDokument: String,
    val variantformat: String
)

data class Sak(
    val sakstype: Sakstype,
    val fagsaksystem: FagsaksSystem? = null,
    val fagsakid: String? = null,
    val arkivsaksystem: String? = null,
    val arkivsaksnummer: String? = null
)

enum class Sakstype {
    FAGSAK,
    GENERELL_SAK,
    ARKIVSAK
}

enum class FagsaksSystem {
    AO01,
    AO11,
    BISYS,
    FS36,
    FS38,
    IT01,
    K9,
    OB36,
    OEBS,
    PP01,
    UFM,
    BA,
    EF,
    KONT,
    SUPSTONAD,
    OMSORGSPENGER
}

//Always use FNR according to #team_dokumentløsninger
private const val ID_TYPE = "FNR"

data class Bruker(
    val id: String,
    val idType: String = ID_TYPE
)

data class AvsenderMottaker(
    val id: String,
    val idType: String = ID_TYPE,
    val navn: String
)

data class KlageApiJournalpost(
    val id: String
)
