package no.nav.klage.common

import io.micrometer.core.instrument.MeterRegistry
import no.nav.klage.util.getLogger
import org.springframework.stereotype.Component

@Component
class KlageMetrics(private val meterRegistry: MeterRegistry) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)

        private const val COUNTER_KLAGER_ARKIVERT = "klager_arkivert"
    }

    fun incrementKlagerArkivert() {
        try {
            meterRegistry.counter(COUNTER_KLAGER_ARKIVERT).increment()
        } catch (e: Exception) {
            logger.warn("incrementKlagerArkivert failed", e)
        }
    }
}