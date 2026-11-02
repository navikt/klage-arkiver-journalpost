package no.nav.klage.util

import no.nav.klage.auth.TexasClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class TokenUtil(
    private val texasClient: TexasClient,
    @Value($$"api://${DOKARKIV_SCOPE}/.default")
    private val dokarkivTarget: String,
    @Value($$"api://${NAIS_CLUSTER_NAME}.${NAIS_NAMESPACE}.${KLAGE_FILE_API_APP_NAME}/.default")
    private val klageFileApiTarget: String,
    @Value($$"api://${NAIS_CLUSTER_NAME}.${NAIS_NAMESPACE}.${KLAGE_DITTNAV_API_APP_NAME}/.default")
    private val klageDittnavApiTarget: String,
    @Value($$"api://${PDL_SCOPE}/.default")
    private val pdlTarget: String,
    @Value($$"api://${NAIS_CLUSTER_NAME}.${NAIS_NAMESPACE}.${KLAGE_LOOKUP_APP_NAME}/.default")
    private val klageLookupTarget: String,
) {
    fun getAppAccessTokenWithDokarkivScope(): String = texasClient.machineToMachineToken(target = dokarkivTarget)

    fun getAppAccessTokenWithKlageFileApiScope(): String = texasClient.machineToMachineToken(target = klageFileApiTarget)

    fun getAppAccessTokenWithKlageDittnavApiScope(): String = texasClient.machineToMachineToken(target = klageDittnavApiTarget)

    fun getAppAccessTokenWithPdlScope(): String = texasClient.machineToMachineToken(target = pdlTarget)

    fun getAppAccessTokenWithKlageLookupScope(): String = texasClient.machineToMachineToken(target = klageLookupTarget)
}
