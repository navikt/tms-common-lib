package no.nav.tms.common.util.config

internal object SystemWrapper {
    fun getEnvVar(varName: String): String? {
        return System.getenv(varName)
    }
}
