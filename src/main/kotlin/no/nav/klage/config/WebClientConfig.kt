package no.nav.klage.config

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.time.Duration
import java.util.concurrent.TimeUnit

@Configuration
class WebClientConfig {

    companion object {
        // Timeouts for different service types
        const val LARGE_FILE_UPLOAD_TIMEOUT_SECONDS = 220L  // dokarkiv - large file uploads (supports 200s+ uploads)
        const val SMALL_FILE_UPLOAD_TIMEOUT_SECONDS = 25L   // dokarkiv - small file uploads (faster failure detection)
        const val FILE_API_TIMEOUT_SECONDS = 60L            // file-api - file operations
        const val STANDARD_TIMEOUT_SECONDS = 30L            // saf, dokdist
        const val FAST_LOOKUP_TIMEOUT_SECONDS = 10L         // pdl - quick lookups
        const val CONNECT_TIMEOUT_MILLIS = 5_000
    }

    @Bean
    fun connectionProvider(): ConnectionProvider {
        return ConnectionProvider.builder("custom")
            // Max idle time - evict connections that have been idle for too long
            .maxIdleTime(Duration.ofSeconds(20))
            // Max life time - evict connections regardless of activity after this time
            .maxLifeTime(Duration.ofMinutes(4))
            // Periodically check and evict connections that have been idle
            .evictInBackground(Duration.ofSeconds(30))
            .build()
    }

    /**
     * HttpClient for standard operations - 30 second timeout.
     */
    @Bean
    fun standardHttpClient(connectionProvider: ConnectionProvider): HttpClient {
        return createHttpClient(
            connectionProvider = connectionProvider,
            timeoutInSeconds = STANDARD_TIMEOUT_SECONDS,
        )
    }

    /**
     * HttpClient for file-api operations - 60 second timeout.
     */
    @Bean
    fun fileApiHttpClient(connectionProvider: ConnectionProvider): HttpClient {
        return createHttpClient(
            connectionProvider = connectionProvider,
            timeoutInSeconds = FILE_API_TIMEOUT_SECONDS,
        )
    }

    /**
     * HttpClient for dokarkiv - supports large file uploads up to 200 seconds.
     * This is needed because document uploads with base64-encoded PDFs can be large.
     */
    @Bean
    fun dokarkivLargeFileHttpClient(connectionProvider: ConnectionProvider): HttpClient {
        return createHttpClient(
            connectionProvider = connectionProvider,
            timeoutInSeconds = LARGE_FILE_UPLOAD_TIMEOUT_SECONDS,
        )
    }

    /**
     * HttpClient for dokarkiv - small file uploads with 25 second timeout.
     * Used for faster failure detection when files are small.
     */
    @Bean
    fun dokarkivSmallFileHttpClient(connectionProvider: ConnectionProvider): HttpClient {
        return createHttpClient(
            connectionProvider = connectionProvider,
            timeoutInSeconds = SMALL_FILE_UPLOAD_TIMEOUT_SECONDS,
        )
    }

    /**
     * HttpClient for fast lookups (pdl, ereg) - 10 second timeout.
     */
    @Bean
    fun fastLookupHttpClient(connectionProvider: ConnectionProvider): HttpClient {
        return createHttpClient(connectionProvider = connectionProvider, timeoutInSeconds = FAST_LOOKUP_TIMEOUT_SECONDS)
    }

    @Bean
    fun dokarkivLargeFileWebClientBuilder(dokarkivLargeFileHttpClient: HttpClient): WebClient.Builder {
        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(dokarkivLargeFileHttpClient))
    }

    @Bean
    fun dokarkivSmallFileWebClientBuilder(dokarkivSmallFileHttpClient: HttpClient): WebClient.Builder {
        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(dokarkivSmallFileHttpClient))
    }

    @Bean
    fun standardWebClientBuilder(standardHttpClient: HttpClient): WebClient.Builder {
        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(standardHttpClient))
    }

    @Bean
    fun fileApiWebClientBuilder(fileApiHttpClient: HttpClient): WebClient.Builder {
        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(fileApiHttpClient))
    }

    @Bean
    fun fastLookupWebClientBuilder(fastLookupHttpClient: HttpClient): WebClient.Builder {
        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(fastLookupHttpClient))
    }

    private fun createHttpClient(connectionProvider: ConnectionProvider, timeoutInSeconds: Long): HttpClient {
        return HttpClient.create(connectionProvider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MILLIS)
            // Enable TCP keep-alive to detect dead connections at OS level
            .option(ChannelOption.SO_KEEPALIVE, true)
            .responseTimeout(Duration.ofSeconds(timeoutInSeconds))
            .doOnConnected { conn ->
                conn.addHandlerLast(ReadTimeoutHandler(timeoutInSeconds, TimeUnit.SECONDS))
                conn.addHandlerLast(WriteTimeoutHandler(timeoutInSeconds, TimeUnit.SECONDS))
            }
    }
}