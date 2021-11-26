package no.nav.klage.service

import no.nav.klage.getLogger
import no.nav.klage.getSecureLogger
import org.springframework.stereotype.Service
import org.verapdf.core.ModelParsingException
import org.verapdf.core.ValidationException
import org.verapdf.pdfa.Foundries
import org.verapdf.pdfa.VeraGreenfieldFoundryProvider
import java.io.ByteArrayInputStream

@Service
class PdfService {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    init {
        VeraGreenfieldFoundryProvider.initialise()
    }

    fun pdfByteArrayIsPdfa(byteArray: ByteArray): Boolean {
        try {
            val parser = Foundries.defaultInstance().createParser(ByteArrayInputStream(byteArray))
            val validator = Foundries.defaultInstance().createValidator(parser.flavour, false)
            val result = validator.validate(parser)
            return result.isCompliant
        } catch (e: ModelParsingException) {
            secureLogger.warn("Error parsing document", e)
        } catch (e: ValidationException) {
            secureLogger.error("Error validating document", e)
            throw e
        }
        return false
    }
}