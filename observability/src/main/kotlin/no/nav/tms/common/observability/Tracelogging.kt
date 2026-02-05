package no.nav.tms.common.observability

import io.github.oshai.kotlinlogging.withLoggingContext
import io.ktor.server.application.*
import io.ktor.server.application.hooks.ResponseSent
import io.ktor.server.request.*
import org.slf4j.MDC


class Contenttype private constructor(val name: String) {
    init {
        require(name.matches(Regex("^[a-z\\-]{4,15}\$"))) {
            "name må være 4-15 tegn og kan kun inneholde småbokstaver og -"
        }
    }

    companion object {
        val utkast = Contenttype("utkast")
        val varsel = Contenttype("varsel")
        val microfrontend = Contenttype("microfrontend")

        /**
         * Oppretter en custom Contenttype.NB! Kun for innhold som ikke er utkast, varsel eller microfrontend.
         * @param name  "verdi i`contenttype`feltet i loggene.Må være 1-15 tegn og kan kun inneholde småbokstaver og -"
         */
        fun custom(name: String) {
            require(
                name.lowercase().contains("utkast").not() &&
                        name.lowercase().contains("varsel").not() &&
                        name.lowercase().contains("microfrontend").not() &&
                        name.lowercase().contains("mikrofrontend").not()
            ) {
                "Bruk predefinerte Contenttype for utkast, varsel eller microfrontend"
            }
            Contenttype(name)
        }
    }
}

/**
 * Interface for å definere kontekst for MinSide-logger. Kan implementeres av objekter som skal logges for å enkelt kunne legge til relevant kontekst i loggene.
 * Inneholder predefinerte felter minside_id, contenttype og produced_by, samt mulighet for ekstra felter ved behov.
 */

interface MinSideContext {
    val contenttype: Contenttype
    val producedBy: String
    val extraFields: Map<String, String>?
    val minSideId: String

    fun toMap(): Map<String, String> {
        val basemap = mapOf(
            "minside_id" to minSideId,
            "contenttype" to contenttype.name,
            "produced_by" to producedBy
        )

        return extraFields?.let {
            return basemap + it
        } ?: basemap
    }

}


/**
 * Legg til kontekst på loki-logger
 * @param [minSideId] id for sporing gjennom loggene fra alle tjenester
 * @param[producedBy] team som produserte innholdet
 * @param[contenttype] type innhold som logges, f.eks varsel, utkast eller microfrontend. Kan også opprettes custom ved behov
 *@param[function] kodeblokk som skal kjøres med denne konteksten
 */
fun withMinSideLoggContext(
    minSideId: String,
    contenttype: Contenttype,
    producedBy: String,
    function: () -> Unit
) {
    withLoggingContext(
        restorePrevious = false,
        map = mapOf(
            "minside_id" to minSideId,
            "contenttype" to contenttype.name,
            "produced_by" to producedBy
        )
    ) { function() }
}

/**
 * Legg til kontekst for loki-logger fra et MinSideContext objekt
 * @param[minSideContext] objekt som implementerer MinSideContext
 */

fun <T : MinSideContext> withMinSideLoggContext(
    minSideContext: T, function: () -> Unit
) {
    withLoggingContext(
        restorePrevious = false,
        map = minSideContext.toMap()
    ) { function() }
}


/**
 * MDC context for API kall med predefinerte felter.
 * @param[route] kallens route
 * @param[contenttype] type innhold som håndteres i kallet
 * @param[method] HTTP metode (GET, POST, etc)
 */
suspend fun withMinSideApiContex(
    route: String,
    contenttype: Contenttype,
    method: String = "GET",
    function: suspend () -> Unit
) {
    withLoggingContext(
        restorePrevious = false,
        map = mapOf(
            "route" to "$method – $route",
            "contenttype" to contenttype.name
        )
    ) { function() }
}


val ApiMdc = createApplicationPlugin(name = "ApiMdc2") {
    onCall { call ->
        MDC.put("route", call.request.uri)
    }
    on(ResponseSent) {
        MDC.remove("route")
    }
}