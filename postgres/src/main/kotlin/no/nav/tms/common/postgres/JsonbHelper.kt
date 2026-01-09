package no.nav.tms.common.postgres

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import org.postgresql.util.PGobject
import kotlin.apply
import kotlin.let

object JsonbHelper {
    val defaultMapper: ObjectMapper = jacksonMapperBuilder()
        .addModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()
        .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)

    fun Any?.toJsonb(objectMapper: ObjectMapper? = null): PGobject? {
        return if (this == null) {
            null
        } else {
            val mapper = objectMapper ?: defaultMapper

            mapper.writeValueAsString(this).let {
                PGobject().apply {
                    type = "jsonb"
                    value = it
                }
            }
        }
    }

    inline fun <reified T> Row.json(label: String, objectMapper: ObjectMapper? = null): T {
        val mapper = objectMapper ?: defaultMapper

        return mapper.readValue(string(label))
    }

    inline fun <reified T> Row.jsonOrNull(label: String, objectMapper: ObjectMapper? = null): T? {
        val mapper = objectMapper ?: defaultMapper

        return stringOrNull(label)?.let { mapper.readValue(it) }
    }
}
