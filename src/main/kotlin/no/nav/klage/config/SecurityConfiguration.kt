package no.nav.klage.config

import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.context.annotation.Configuration

@EnableJwtTokenValidation(ignore = ["org.springdoc"])
@EnableOAuth2Client(cacheEnabled = true)
@Configuration
internal class SecurityConfiguration
