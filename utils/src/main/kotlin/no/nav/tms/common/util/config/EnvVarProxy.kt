package no.nav.tms.common.util.config

internal object EnvVarProxy {
    fun getEnvVar(varName: String): String? {
        return System.getenv(varName)
    }
}
