package no.nav.tms.common.util.config

import no.nav.tms.common.util.config.TypedEnvVar.getEnvVarAsType
import no.nav.tms.common.util.config.TypedEnvVar.getOptionalEnvVarAsType
import java.net.URI
import java.net.URL

object UrlEnvVar {
    fun getEnvVarAsURL(varName: String, default: URL? = null, trimTrailingSlash: Boolean = false): URL {
        return getEnvVarAsType(varName, default) { envVar ->
            if (trimTrailingSlash) {
                createUrl(envVar.trimEnd('/'))
            } else {
                createUrl(envVar)
            }
        }
    }

    fun getOptionalEnvVarAsURL(varName: String, default: URL? = null, trimTrailingSlash: Boolean = false): URL? {
        return getOptionalEnvVarAsType(varName, default) { envVar ->
            if (trimTrailingSlash) {
                createUrl(envVar.trimEnd('/'))
            } else {
                createUrl(envVar)
            }
        }
    }
}

private fun createUrl(uri: String) = URI.create(uri).toURL()
