package no.nav.klage.service

import no.nav.klage.util.getLogger
import no.nav.klage.util.getTeamLogger
import org.springframework.stereotype.Service
import org.verapdf.core.ModelParsingException
import org.verapdf.core.ValidationException
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider
import org.verapdf.pdfa.Foundries
import java.io.ByteArrayInputStream

@Service
class PdfService {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val teamLogger = getTeamLogger()
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
            logger.warn("Error parsing document. See more in team-logs.")
            teamLogger.warn("Error parsing document", e)
        } catch (e: ValidationException) {
            logger.warn("Error validating document. See more in team-logs.")
            teamLogger.warn("Error validating document", e)
        }
        return false
    }
}