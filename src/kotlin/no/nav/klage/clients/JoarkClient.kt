package no.nav.klage.clients

import no.nav.klage.domain.Klage
import org.intellij.lang.annotations.Language
import org.springframework.stereotype.Component
import java.util.*

@Component
class JoarkClient {

    fun createJournalpost(klagePDFBytes: ByteArray, klage: Klage) {
        //TODO Call joark
        @Language("JSON")
        val json = """
         {
          "avsenderMottaker": {
            "id": ${klage.foedselsnummer},
            "idType": "FNR",
            "navn": "etternavn, fornavn, mellomnavn"
          },
          "bruker": {
            "id": ${klage.foedselsnummer},
            "idType": "FNR"
          },
          "dokumenter": [
            {
              "dokumentKategori": "VB",
              "dokumentvarianter": [
                {
                  "filtype": "PDFA",
                  "fysiskDokument": "${Base64.getEncoder().encodeToString(klagePDFBytes)}",
                  "variantformat": "ARKIV"
                },
                {
                  "filtype": "JSON",
                  "fysiskDokument": "${Base64.getEncoder().encodeToString(klage.toString().toByteArray())}",
                  "variantformat": "ORIGINAL"
                }
              ],
              "tittel": "Klage"
            }
          ],
          "eksternReferanseId": "correlationId",
          "journalfoerendeEnhet": 9999,
          "journalpostType": "UTGAAENDE",
          "sak": {
            "sakstype": "GENERELL_SAK"
          },
          "tema": "SYK",
          "tittel": "Klage"
        }
        """.trimIndent()
    }
}