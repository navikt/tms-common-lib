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
 * Legg til kontekst for på loki-logger
 * @param[minSideId] unik id for innholdet
 * @param[producedBy] team som produserte innholdet
 */

fun withMDC(
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
 * MDC context for API kall med predefinerte felter.
 * @param[route] kallens route
 * @param[contenttype] type innhold som håndteres i kallet
 * @param[method] HTTP metode (GET, POST, etc)
 */

suspend fun withApiTracing(
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
