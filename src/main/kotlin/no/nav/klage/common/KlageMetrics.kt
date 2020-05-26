package no.nav.klage.common

import io.micrometer.core.instrument.MeterRegistry
import no.nav.klage.getLogger
import org.springframework.stereotype.Component

@Component
class KlageMetrics(private val meterRegistry: MeterRegistry) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)

        private const val COUNTER_KLAGER_ARKIVERT = "klager.arkivert"
        private const val COUNTER_VEDLEGG = "klager.vedlegg"
    }

    fun incrementKlager() {
        try {
            meterRegistry.counter(COUNTER_KLAGER_ARKIVERT).increment()
        } catch (e: Exception) {
            logger.warn("incrementKlager failed", e)
        }
    }

    fun incrementVedlegg(amount: Int) {
        try {
            meterRegistry.counter(COUNTER_VEDLEGG).increment(amount.toDouble())
        } catch (e: Exception) {
            logger.warn("incrementVedlegg failed", e)
        }
    }
}