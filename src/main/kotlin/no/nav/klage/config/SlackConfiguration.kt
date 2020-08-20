package no.nav.klage.config

import no.nav.klage.clients.SlackClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SlackConfiguration {
    @Value("\${SLACK_ACCESS_TOKEN}")
    lateinit var accessToken: String

    @Value("\${SLACK_CHANNEL_ID}")
    lateinit var channelId: String

    @Bean
    fun slackClient(): SlackClient = SlackClient(accessToken, channelId)
}
