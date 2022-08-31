package no.nav.klage.config

import no.nav.klage.getLogger
import no.nav.klage.getSecureLogger
import no.nav.slackposter.Severity
import no.nav.slackposter.SlackClient
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff
import java.time.Duration


@Configuration
class AivenKafkaConfiguration(
    @Value("\${KAFKA_BROKERS}")
    private val kafkaBrokers: String,
    @Value("\${KAFKA_TRUSTSTORE_PATH}")
    private val kafkaTruststorePath: String,
    @Value("\${KAFKA_CREDSTORE_PASSWORD}")
    private val kafkaCredstorePassword: String,
    @Value("\${KAFKA_KEYSTORE_PATH}")
    private val kafkaKeystorePath: String,
    private val slackClient: SlackClient,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    @Bean
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.consumerFactory = klageMottattConsumerFactory()

        //Setup sending to dead-letter topic after two retries
        val recoverer = DeadLetterPublishingRecoverer(aivenKafkaTemplate()) f@
        { r, _ ->
            val dltTopic = r.topic().toString() + "-dlt"
            logger.debug("Message could not be processed and will be sent to DLT: {}", dltTopic)
            slackClient.postMessage("Innsending av klage feilet og vil nå bli lagt på DLT", Severity.ERROR)
            return@f TopicPartition(
                dltTopic,
                r.partition()
            )
        }
        factory.setCommonErrorHandler(
            DefaultErrorHandler(recoverer, FixedBackOff(0L, 2L))
        )

        //Retry consumer/listener even if authorization fails at first
        factory.setContainerCustomizer { container ->
            container.containerProperties.setAuthExceptionRetryInterval(Duration.ofSeconds(10L))
        }

        return factory
    }

    @Bean
    fun aivenKafkaTemplate(): KafkaTemplate<String, String> {
        val config = mapOf(
            ProducerConfig.CLIENT_ID_CONFIG to "klage-arkiver-journalpost-producer",
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
        ) + commonConfig()

        return KafkaTemplate(DefaultKafkaProducerFactory(config))
    }

    @Bean
    fun klageMottattConsumerFactory(): ConsumerFactory<String, String> {
        val config = mapOf(
            ConsumerConfig.GROUP_ID_CONFIG to "klage-arkiver-journalpost",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to true,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG to Duration.ofDays(3).toMillis().toInt(),
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
        ) + commonConfig()

        return DefaultKafkaConsumerFactory(config)
    }

    //Common
    private fun commonConfig() = mapOf(
        BOOTSTRAP_SERVERS_CONFIG to kafkaBrokers
    ) + securityConfig()

    private fun securityConfig() = mapOf(
        CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SSL",
        SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG to "", // Disable server host name verification
        SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG to "JKS",
        SslConfigs.SSL_KEYSTORE_TYPE_CONFIG to "PKCS12",
        SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to kafkaTruststorePath,
        SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to kafkaCredstorePassword,
        SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to kafkaKeystorePath,
        SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to kafkaCredstorePassword,
        SslConfigs.SSL_KEY_PASSWORD_CONFIG to kafkaCredstorePassword,
    )

}
