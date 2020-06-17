package no.nav.klage.clients

import no.nav.klage.domain.Klage
import no.nav.klage.service.ApplicationService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class TestController(private val applicationService: ApplicationService) {

    @GetMapping("trigger")
    fun trigger() {
        val klage = Klage(
            id = 1,
            klageInstans = true,
            trygderetten = false,
            navn = "Anka, Kalle",
            adresse = "En adresse 1",
            telefon = "12345678",
            navenhet = "123",
            vedtaksdato = LocalDate.now(),
            navReferanse = "navRef",
            kortRedegjoerelse = "kort tekst",
            dato = LocalDate.now(),
            oversiktVedlegg = "ingen vedlegg",
            begrunnelse = "lang begrunnelse",
            identifikasjonsnummer = "10108000398",
            identifikasjonstype = "FNR",
            tema = "SYK"
        )
        applicationService.createJournalpost(klage)
    }
}