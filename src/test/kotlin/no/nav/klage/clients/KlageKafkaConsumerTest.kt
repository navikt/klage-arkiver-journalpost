package no.nav.klage.clients

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.klage.domain.KlageAnkeInput
import no.nav.klage.domain.Vedlegg
import no.nav.klage.domain.toKlage
import no.nav.klage.service.ApplicationService
import no.nav.slackposter.Severity
import no.nav.slackposter.SlackClient
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.MockitoAnnotations
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate

@ExtendWith(SpringExtension::class)
class KlageKafkaConsumerTest {

    @MockkBean
    lateinit var applicationService: ApplicationService

    @MockkBean
    lateinit var slackClient: SlackClient

    @MockkBean
    lateinit var klageDittnavAPIClient: KlageDittnavAPIClient

    lateinit var klageKafkaConsumer: KlageKafkaConsumer

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
        klageKafkaConsumer = KlageKafkaConsumer(
            applicationService,
            slackClient,
            klageDittnavAPIClient
        )
    }

    data class OldKlage(
        val id: Int,
        val identifikasjonstype: String,
        val identifikasjonsnummer: String,
        val klageInstans: Boolean,
        val trygderetten: Boolean,
        val fornavn: String,
        val mellomnavn: String,
        val etternavn: String,
        val adresse: String,
        val telefon: String,
        val vedtak: String,
        val saksnummer: String?,
        val dato: LocalDate,
        val begrunnelse: String,
        val tema: String,
        val ytelse: String,
        val vedlegg: List<Vedlegg> = emptyList(),
        var fileContentAsBytes: ByteArray? = null
    )

//    data class KlageAnkeInput(
//        val id: Int,
//        val identifikasjonstype: String,
//        val identifikasjonsnummer: String,
//        val klageInstans: Boolean?,
//        val trygderetten: Boolean?,
//        val fornavn: String,
//        val mellomnavn: String,
//        val etternavn: String,
//        val adresse: String,
//        val telefon: String,
//        val vedtak: String,
//        val dato: LocalDate,
//        val begrunnelse: String,
//        val tema: String,
//        val ytelse: String,
//        val vedlegg: List<Vedlegg> = emptyList(),
//        var fileContentAsBytes: ByteArray? = null,
//        //deprecated, only used to parse old kafka entries
//        val saksnummer: String? = null,
//        val userChoices: List<String>? = emptyList(),
//        val userSaksnummer: String?,
//        val internalSaksnummer: String?,
//        val fullmektigNavn: String?,
//        val fullmektigFnr: String?,
//        val klageAnkeType: KlageAnkeType = no.nav.klage.domain.KlageAnkeType.KLAGE,
//        val previousUtfall: String?
//    )

    val oldKlageInput = OldKlage(
        id = 123,
        identifikasjonstype = "FNR",
        identifikasjonsnummer = "12345678901",
        klageInstans = false,
        trygderetten = false,
        fornavn = "Navn",
        mellomnavn = "",
        etternavn = "Navnesen",
        adresse = "Gateveien 123",
        telefon = "98765432",
        vedtak = "",
        dato = LocalDate.now(),
        begrunnelse = "BEGRUNNELSE",
        tema = "OMS",
        ytelse = "OMS",
        saksnummer = "123"
    )

    val input = KlageAnkeInput(
        id = 123,
        identifikasjonstype = "FNR",
        identifikasjonsnummer = "12345678901",
        klageInstans = null,
        trygderetten = null,
        fornavn = "Navn",
        mellomnavn = "",
        etternavn = "Navnesen",
        adresse = "Gateveien 123",
        telefon = "98765432",
        vedtak = "",
        dato = LocalDate.now(),
        begrunnelse = "BEGRUNNELSE",
        tema = "OMS",
        ytelse = "OMS",
        userSaksnummer = null,
        internalSaksnummer = null,
        fullmektigNavn = null,
        fullmektigFnr = null,
        previousUtfall = null
    )

    val mapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerModule(KotlinModule())

    @Test
    fun `Innsending med utdatert modell blir deserialisert`() {
        val inputString = mapper
            .writeValueAsString(oldKlageInput)
        val parsedKlage = inputString.toKlage()
        assertEquals(parsedKlage.id, oldKlageInput.id)
    }

    @Nested
    inner class GammelModell {
        @Test
        fun `utdatert modell - klageKafkaConsumer går ikke videre til createJournalpost`() {
            val inputString = mapper
                .writeValueAsString(oldKlageInput)

            every { slackClient.postMessage(any(), any()) } returns Unit
            every { klageDittnavAPIClient.getJournalpostForKlageId(any()) } returns JournalpostIdResponse(journalpostId = "321")

            klageKafkaConsumer.listen(ConsumerRecord("aapen-klager-klageOpprettet", 0, 0, "test", inputString))

            verify(exactly = 0) { applicationService.createJournalpost(any()) }
        }

        @Test
        fun `utdatert modell - dersom klageDittnavAPI ikke gir journalpostID så varsles det i slack`() {
            val inputString = mapper
                .writeValueAsString(oldKlageInput)

            every { slackClient.postMessage(any(), any()) } returns Unit
            every { klageDittnavAPIClient.getJournalpostForKlageId(any()) } returns JournalpostIdResponse(journalpostId = null)

            assertThrows<RuntimeException> {
                klageKafkaConsumer.listen(
                    ConsumerRecord(
                        "aapen-klager-klageOpprettet",
                        0,
                        0,
                        "test",
                        inputString
                    )
                )
            }

            verify(exactly = 0) { applicationService.createJournalpost(any()) }
            verify {
                slackClient.postMessage(
                    "Innsending med id ${oldKlageInput.id} har ikke journalpostId. Undersøk dette nærmere!",
                    Severity.ERROR
                )
            }
        }
    }

    @Nested
    inner class NyModell {
        @Test
        fun `Dersom klageDittnavAPI gir journalpostID så går ikke klageKafkaConsumer videre til createJournalpost`() {
            val inputString = mapper
                .writeValueAsString(input)

            every { slackClient.postMessage(any(), any()) } returns Unit
            every { klageDittnavAPIClient.getJournalpostForKlageId(any()) } returns JournalpostIdResponse(journalpostId = "321")

            klageKafkaConsumer.listen(ConsumerRecord("aapen-klager-klageOpprettet", 0, 0, "test", inputString))

            verify(exactly = 0) { applicationService.createJournalpost(any()) }
        }

        @Test
        fun `Dersom klageDittnavAPI ikke gir journalpostID så går klageKafkaConsumer videre til createJournalpost`() {
            val inputString = mapper
                .writeValueAsString(input)

            every { slackClient.postMessage(any(), any()) } returns Unit
            every { klageDittnavAPIClient.getJournalpostForKlageId(any()) } returns JournalpostIdResponse(journalpostId = null)
            every { applicationService.createJournalpost(any()) } returns Unit

            klageKafkaConsumer.listen(ConsumerRecord("aapen-klager-klageOpprettet", 0, 0, "test", inputString))

            verify(exactly = 1) { applicationService.createJournalpost(any()) }
        }
    }
}