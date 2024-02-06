package no.nav.tms.common.util.config

import java.lang.Exception
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException

/**
 * Utility functions for fetching environment values parseable as types other than String
 */
internal object TypedEnvVar {
    internal inline fun <reified T> getEnvVarAsType(varName: String, default: T? = null, mapper: (String) -> T): T {
        return EnvVarProxy.getEnvVar(varName)
            ?.applyMapper(mapper)
            ?: default
            ?: throw IllegalStateException("Appen kan ikke starte uten av miljøvariabelen $varName er satt.")
    }

    internal inline fun <reified T> getOptionalEnvVarAsType(varName: String, default: T? = null, mapper: (String) -> T): T? {
        return EnvVarProxy.getEnvVar(varName)
            ?.applyMapper(mapper)
            ?: default
    }

    internal inline fun <reified T> getEnvVarAsTypedList(
        varName: String,
        default: List<T>? = null,
        separator: String = ",",
        mapper: (String) -> T
    ): List<T> {
        return EnvVarProxy.getEnvVar(varName)
            ?.split(separator)
            ?.map { listEntry -> listEntry.applyMapper(mapper) }
            ?: default
            ?: throw IllegalStateException("Appen kan ikke starte uten av miljøvariabelen $varName er satt.")
    }

    internal inline fun <reified T> getOptionalEnvVarAsTypedList(
        varName: String,
        default: List<T>? = null,
        separator: String = ",",
        mapper: (String) -> T
    ): List<T> {
        return EnvVarProxy.getEnvVar(varName)
            ?.split(separator)
            ?.map { listEntry -> listEntry.applyMapper(mapper) }
            ?: default
            ?: emptyList()
    }

    private inline fun <reified T> String.applyMapper(mapper: (String) -> T): T {
        return try {
            mapper(this)
        } catch (e: Exception) {
            throw IllegalArgumentException("Klarte ikke konvertere variabel $this til ${T::class.simpleName}")
        }
    }
}
