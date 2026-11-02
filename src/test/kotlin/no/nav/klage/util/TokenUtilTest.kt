package no.nav.klage.util

import io.mockk.every
import io.mockk.mockk
import no.nav.klage.auth.TexasClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TokenUtilTest {
    private val texasClient = mockk<TexasClient>()

    private val tokenUtil =
        TokenUtil(
            texasClient = texasClient,
            dokarkivTarget = "dokarkiv",
            klageFileApiTarget = "klage-file-api",
            klageDittnavApiTarget = "klage-dittnav-api",
            pdlTarget = "pdl",
            klageLookupTarget = "klage-lookup",
        )

    init {
        every { texasClient.machineToMachineToken(target = any()) } answers { "token-for-${firstArg<String>()}" }
    }

    @Test
    fun `each method asks Texas for a machine-to-machine token with its own target`() {
        assertThat(tokenUtil.getAppAccessTokenWithDokarkivScope()).isEqualTo("token-for-dokarkiv")
        assertThat(tokenUtil.getAppAccessTokenWithKlageFileApiScope()).isEqualTo("token-for-klage-file-api")
        assertThat(tokenUtil.getAppAccessTokenWithKlageDittnavApiScope()).isEqualTo("token-for-klage-dittnav-api")
        assertThat(tokenUtil.getAppAccessTokenWithPdlScope()).isEqualTo("token-for-pdl")
        assertThat(tokenUtil.getAppAccessTokenWithKlageLookupScope()).isEqualTo("token-for-klage-lookup")
    }
}
