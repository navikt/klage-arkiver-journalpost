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
            navenhet = "enNavenhet",
            vedtaksdato = LocalDate.now(),
            navReferanse = "navRef",
            kortRedegjoerelse = "kort tekst",
            sted = "Oslo",
            dato = LocalDate.now(),
            oversiktVedlegg = "ingen vedlegg",
            begrunnelse = "lang begrunnelse",
            foedselsnummer = "12345678910",
            tema = "SYK"
        )
        applicationService.createJournalpost(klage)
    }
}