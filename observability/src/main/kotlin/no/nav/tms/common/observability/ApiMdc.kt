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

class Domain private constructor(val name: String, val addToMdc: () -> Unit = { MDC.put("domain", name) }) {
    init {
        require(name.matches(Regex("^[a-z\\-]{4,15}\$"))) {
            "name må være 4-15 tegn og kan kun inneholde småbokstaver og -"
        }
    }

    companion object {
        val utkast = Domain("utkast")
        val varsel = Domain("varsel")
        val microfrontend = Domain("microfrontend")
        val none: Domain = Domain("none", { MDC.remove("domain") })

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
            value.addToMdc.invoke()
        }
    }


var Route.mdcDomain: Domain?
    get() = attributes.getOrNull(ROUTE_MDC_DOMAIN_KEY)
    set(value) {
        if (value != null) {
            attributes.put(ROUTE_MDC_DOMAIN_KEY, value)
        }
    }


private val MDC_CONTEXT_MAP_KEY = AttributeKey<Map<String, String>>("mdcContextMap")

/**
 * Ktor-plugin som legger til MDC-felt for route, method og domain på alle requests.
 *
 * MDC-felt som settes:
 *  - `route`: Request-URI (f.eks. /api/varsler)
 *  - `method`: HTTP-metode (GET, POST, ...)
 *  - `domain`: Domenet requesten tilhører (settes via [MdcDomainConfig])
 *
 * Hvordan konfigurere
 * - Når du installerer pluginen kan du gi et standarddomene for hele appen:
 *   `install(ApiMdc) { applicationDomain = Domain.varsel }`
 *
 * Hvordan overstyre domenet
 * - Sett `route.mdcDomain = Domain.custom("navn")` for en hel route.
 * - Sett `call.mdcDomain = Domain.custom("navn")` inne i en route for å overstyre for ett kall.
 * - Sett `call.mdcDomain = Domain.none` for å fjerne `domain` fra MDC for det kallet.
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

    val appDomain = pluginConfig.applicationDomain

     onCall { call ->
        val mdcContextMap = mutableMapOf(
            "route" to call.request.uri,
            "method" to call.request.httpMethod.value
        )
        appDomain?.let { mdcContextMap["domain"] = it.name }

        call.attributes.put(MDC_CONTEXT_MAP_KEY, mdcContextMap)
        mdcContextMap.forEach { (key, value) -> MDC.put(key, value) }
    }

    onCallRespond { call, _ ->
        call.attributes.getOrNull(MDC_CONTEXT_MAP_KEY)?.forEach { (key, value) ->
            MDC.put(key, value)
        }
    }

    on(ResponseSent) {
        MDC.clear()
    }

    on(MonitoringEvent(RoutingRoot.RoutingCallStarted)) { call ->
        val routeDomain = call.route.findMdcDomain()
        if (routeDomain != null) {
            call.attributes.put(ROUTE_MDC_DOMAIN_KEY, routeDomain)
            routeDomain.addToMdc()
        }
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
}
