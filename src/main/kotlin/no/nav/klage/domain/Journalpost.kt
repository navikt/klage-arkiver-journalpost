package no.nav.klage.domain

data class Journalpost(
    val journalposttype: String = "INNGAAENDE",
    val tema: String,
    val kanal: String = "NAV_NO",
    val tittel: String,
    val avsenderMottaker: AvsenderMottaker,
    val bruker: Bruker,
    val sak: Sak? = null,
    val eksternReferanseId: String,
    val dokumenter: List<Dokument> = mutableListOf(),
    val tilleggsopplysninger: List<Tilleggsopplysning> = mutableListOf()
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
    val sakstype: String,
    val fagsaksystem: String? = null,
    val fagsakid: String? = null,
    val arkivsaksystem: String? = null,
    val arkivsaksnummer: String? = null
)

data class Bruker(
    val id: String,
    val idType: String
)

data class AvsenderMottaker(
    val id: String,
    val idType: String,
    val navn: String
)
