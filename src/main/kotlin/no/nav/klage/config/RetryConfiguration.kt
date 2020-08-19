package no.nav.klage.config

import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.retry.RetryRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class RetryConfiguration {

    private val retryConfig: RetryConfig = RetryConfig
        .custom<RetryConfig>()
        .maxAttempts(5)
        .waitDuration(Duration.ofSeconds(3))
        .retryExceptions(RuntimeException::class.java)
        .build()

    private val retryRegistry = RetryRegistry.of(retryConfig)

    @Bean
    fun retryPdf(): Retry = retryRegistry.retry("PdfGenerator")

}
