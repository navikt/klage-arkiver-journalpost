package no.nav.klage.service

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path

@Disabled
class PdfServiceTest {
    private val pdfService = PdfService()

    @Test
    fun `pdf-a file is valid`() {
        val multipartFileMock = mockk<MultipartFile>()
        every { multipartFileMock.bytes } returns Files.readAllBytes(
            Path.of("src/test/resources/pdf/pdf-a.pdf")
        )
        assertTrue(pdfService.pdfByteArrayIsPdfa(multipartFileMock.bytes))
    }

    @Test
    fun `non-compliant pdf file is not valid`() {
        val multipartFileMock = mockk<MultipartFile>()
        every { multipartFileMock.bytes } returns Files.readAllBytes(
            Path.of("src/test/resources/pdf/not-pdf-a.pdf")
        )
        assertFalse(pdfService.pdfByteArrayIsPdfa(multipartFileMock.bytes))
    }
}