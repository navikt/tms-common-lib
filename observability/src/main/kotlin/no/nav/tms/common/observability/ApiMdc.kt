package no.nav.tms.common.observability

import io.github.oshai.kotlinlogging.withLoggingContext
import io.ktor.server.application.*
import io.ktor.server.application.hooks.ResponseSent
import io.ktor.server.request.*
import org.slf4j.MDC


class Domain private constructor(val name: String) {
    init {
        require(name.matches(Regex("^[a-z\\-]{4,15}\$"))) {
            "name må være 4-15 tegn og kan kun inneholde småbokstaver og -"
        }
    }

    companion object {
        val utkast = Domain("utkast")
        val varsel = Domain("varsel")
        val microfrontend = Domain("microfrontend")

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
            Domain(name)
        }
    }
}

/**
 * MDC context for API kall med predefinerte felter.
 * @param[route] kallens route
 * @param[domain] type innhold som håndteres i kallet
 * @param[method] HTTP metode (GET, POST, etc)
 */
suspend fun withMinSideApiContex(
    route: String,
    domain: Domain,
    method: String = "GET",
    function: suspend () -> Unit
) {
    withLoggingContext(
        restorePrevious = false,
        map = mapOf(
            "route" to "$method – $route",
            "domain" to domain.name
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