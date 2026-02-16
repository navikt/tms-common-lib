package no.nav.tms.common.observability

import io.ktor.server.application.*
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.application.hooks.ResponseSent
import io.ktor.server.request.*
import io.ktor.util.AttributeKey
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingRoot
import org.slf4j.MDC



private val MDC_DOMAIN_KEY = AttributeKey<Domain>("mdcDomain")
private val ROUTE_MDC_DOMAIN_KEY = AttributeKey<Domain>("routeMdcDomain")

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
        val mixed: Domain? = null

        /**
         * Oppretter en custom Contenttype.NB! Kun for innhold som ikke er utkast, varsel eller microfrontend.
         * @param name  "verdi i`contenttype`feltet i loggene.Må være 1-15 tegn og kan kun inneholde småbokstaver og -"
         */
        fun custom(name: String): Domain {
            require(
                name.lowercase().contains("utkast").not() &&
                        name.lowercase().contains("varsel").not() &&
                        name.lowercase().contains("microfrontend").not() &&
                        name.lowercase().contains("mikrofrontend").not()
            ) {
                "Bruk predefinerte Contenttype for utkast, varsel eller microfrontend"
            }
            return Domain(name)
        }
    }
}



var ApplicationCall.mdcDomain: Domain?
    get() = attributes.getOrNull(MDC_DOMAIN_KEY)
    set(value) {
        if (value != null) {
            attributes.put(MDC_DOMAIN_KEY, value)
            MDC.put("domain", value.name)
        }
    }


var Route.mdcDomain: Domain?
    get() = attributes.getOrNull(ROUTE_MDC_DOMAIN_KEY)
    set(value) {
        if (value != null) {
            attributes.put(ROUTE_MDC_DOMAIN_KEY, value)
        }
    }


/**
 * Ktor-plugin som legger til MDC-felt for route, method og domain på alle requests.
 *
 * MDC-felt som settes:
 *  - `route`: Request-URI (f.eks. /api/varsler)
 *  - `method`: HTTP-metode (GET, POST, ...)
 *  - `domain`: Domenet requesten tilhører (settes via [MdcDomainConfig])
 *
 * Konfigurasjon via [MdcDomainConfig]:
 *  - [applicationDomain]: Standard domene for hele applikasjonen
 *  - [routeScopedDomains]: Aktiver domene per route med [mdcDomain] på [Route]
 *  - [methodScopedDomains]: Aktiver domene per request med [mdcDomain] på [ApplicationCall]
 *
 * Eksempel på bruk:
 *
 * ```kotlin
 * install(ApiMdc) {
 *     applicationDomain = Domain.varsel
 * }
 * ```
 *
 * @see Domain
 * @see MdcDomainConfig
 */
val ApiMdc = createApplicationPlugin(name = "ApiMdc", createConfiguration = ::MdcDomainConfig) {
    onCall { call ->
        MDC.put("route", call.request.uri)
        MDC.put("method", call.request.httpMethod.value)
        // Set application-level domain if no scoped domains are enabled
        if (!pluginConfig.routeScopedDomains && !pluginConfig.methodScopedDomains) {
            pluginConfig.applicationDomain?.let { MDC.put("domain", it.name) }
        }
    }
    on(MonitoringEvent(RoutingRoot.RoutingCallStarted)) { call ->
        if (pluginConfig.routeScopedDomains || pluginConfig.methodScopedDomains) {
            val routeDomain = call.route.findMdcDomain()
            if (routeDomain != null) {
                call.attributes.put(ROUTE_MDC_DOMAIN_KEY, routeDomain)
                MDC.put("domain", routeDomain.name)
            }
        }
    }
    on(ResponseSent) {
        MDC.remove("route")
        MDC.remove("method")
        MDC.remove("domain")
    }
}

private fun Route.findMdcDomain(): Domain? {
    var currentRoute: Route? = this
    while (currentRoute != null) {
        currentRoute.attributes.getOrNull(ROUTE_MDC_DOMAIN_KEY)?.let { return it }
        currentRoute = currentRoute.parent
    }
    return null
}

class MdcDomainConfig {
    var applicationDomain: Domain? = null
    var routeScopedDomains: Boolean = false
    var methodScopedDomains: Boolean = false
}
