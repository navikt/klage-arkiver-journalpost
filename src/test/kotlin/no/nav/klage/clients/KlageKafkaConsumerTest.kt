package no.nav.klage.clients

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.klage.domain.KlageAnkeInput
import no.nav.klage.domain.KlageAnkeType
import no.nav.klage.kodeverk.innsendingsytelse.Innsendingsytelse
import no.nav.klage.service.ApplicationService
import no.nav.slackposter.SlackClient
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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

    private val input = KlageAnkeInput(
        id = "123",
        identifikasjonsnummer = "12345678901",
        fornavn = "Navn",
        mellomnavn = "",
        etternavn = "Navnesen",
        vedtak = "",
        dato = LocalDate.now(),
        begrunnelse = "BEGRUNNELSE",
        tema = "OMS",
        ytelse = "OMS",
        userSaksnummer = null,
        internalSaksnummer = null,
        klageAnkeType = KlageAnkeType.KLAGE,
        ettersendelseTilKa = false,
        innsendingsYtelseId = Innsendingsytelse.SYKDOM_I_FAMILIEN.id,
        hoveddokument = null,
    )

    private val mapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerModule(
            KotlinModule.Builder()
                .withReflectionCacheSize(512)
                .configure(KotlinFeature.NullToEmptyCollection, false)
                .configure(KotlinFeature.NullToEmptyMap, false)
                .configure(KotlinFeature.NullIsSameAsDefault, false)
                .configure(KotlinFeature.StrictNullChecks, false)
                .build()
        )


    @Test
    fun `Dersom klageDittnavAPI gir journalpostID så går ikke klageKafkaConsumer videre til createJournalpost`() {
        val inputString = mapper
            .writeValueAsString(input)

        every { slackClient.postMessage(any(), any()) } returns Unit
        every { klageDittnavAPIClient.getJournalpostForKlankeId(any()) } returns JournalpostIdResponse(journalpostId = "321")

        klageKafkaConsumer.listen(ConsumerRecord("klage.privat-klage-mottatt-v1", 0, 0, "test", inputString))

        verify(exactly = 0) { applicationService.createJournalpost(any()) }
    }

    @Test
    fun `Dersom klageDittnavAPI ikke gir journalpostID så går klageKafkaConsumer videre til createJournalpost`() {
        val inputString = mapper
            .writeValueAsString(input)

        every { slackClient.postMessage(any(), any()) } returns Unit
        every { klageDittnavAPIClient.getJournalpostForKlankeId(any()) } returns JournalpostIdResponse(journalpostId = null)
        every { applicationService.createJournalpost(any()) } returns Unit

        klageKafkaConsumer.listen(ConsumerRecord("klage.privat-klage-mottatt-v1", 0, 0, "test", inputString))

        verify(exactly = 1) { applicationService.createJournalpost(any()) }
    }
}