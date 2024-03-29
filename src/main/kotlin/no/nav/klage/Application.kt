package no.nav.klage

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class Application {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @Value("\${DRY_RUN}")
    private lateinit var dryRun: String

    @PostConstruct
    fun postConstruct() {
        if (dryRun.toBoolean()) {
            logger.debug("Dry run mode activated. Will not send journalpost to Joark.")
        } else {
            logger.debug("Application is in production mode and will send journalpost to Joark.")
        }
    }
}



fun main() {
    runApplication<Application>()
}