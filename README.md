# klage-arkiver-journalpost
Konsumerer Kafka-topic for klager og oppretter journalposter i Joark.

## Melding til Joark

Vi oppretter journalposter i Joark ved `POST`-kall mot `opprettJournalpost`, basert på denne dokumentasjonen: https://confluence.adeo.no/display/BOA/opprettJournalpost

Eksempel på melding i kall: 
```
{
  "journalposttype": "INNGAAENDE",
  "tema": "FOR",
  "behandlingstema": "ab0019",
  "kanal": "NAV_NO",
  "tittel": "Klage/Anke",
  "avsenderMottaker": {
    "id": "12345678910",
    "idType": "FNR",
    "navn": "TEST TESTESEN"
  },
  "bruker": {
    "id": "12345678910",
    "idType": "FNR"
  },
  "sak": null,
  "eksternReferanseId": "f7963b0616fe6b45bcbfd2b1a5c70afc",
  "dokumenter": [
    {
      "tittel": "Klage/Anke",
      "brevkode": "NAV 90-00.08",
      "dokumentVarianter": [
        {
          "filtype": "PDFA",
          "fysiskDokument": "base64 data removed for logging purposes",
          "variantformat": "ARKIV"
        }
      ]
    },
    {
      "tittel": "test.jpg",
      "brevkode": null,
      "dokumentVarianter": [
        {
          "filtype": "PDF",
          "fysiskDokument": "base64 data removed for logging purposes",
          "variantformat": "ARKIV"
        }
      ]
    }
  ],
  "tilleggsopplysninger": [
    {
      "nokkel": "klage_id",
      "verdi": "123"
    }
  ]
}
```
Merk:
`behandlingstema` og `brevkode` er satt statisk, og spesifisert for å vise at journalposten gjelder klage. 

Interne henvendelser kan sendes via Slack i kanalen #team-p3-effektiviseringavklageprosessen
