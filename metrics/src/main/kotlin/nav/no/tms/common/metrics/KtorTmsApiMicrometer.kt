package nav.no.tms.common.metrics

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.Counter
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import nav.no.tms.common.metrics.Sensitivity.Companion.resolveSensitivity


fun Application.installTmsMicrometerMetrics(config: TmsMicrometricsConfig.() -> Unit) {
    val metricsConfig = TmsMicrometricsConfig().apply {
        config()
        verify()
    }
    if (metricsConfig.installMicrometerPlugin) {
        log.info("Installerer micrometer plugin for Ktor")
        install(MicrometerMetrics) {
            registry = metricsConfig.registry
        }
    }
    TmsApiMicrometerCounter.config = metricsConfig
    if (metricsConfig.setupMetricsRoute) {
        log.info("installerer /metrics route")
        routing {
            get("metrics") {
                call.respond(metricsConfig.registry.scrape())
            }
        }
    }

    log.info("Installerer TmsMicrometrics for Ktor")

    install(createApplicationPlugin(name = "TmsMicrometrics") {

        on(MonitoringEvent(Routing.RoutingCallStarted)) {
            it.attributes.put(TmsMetricsConfig.routeKey, it.routeStr())
        }

        on(ResponseSent) { call ->
            val route = call.attributes.getOrNull(TmsMetricsConfig.routeKey)?: recordableRoute(call.request)
            val status = call.response.status()

            if (!metricsConfig.excludeRoute(route, status?.value)) {
                TmsApiMicrometerCounter.countApiCall(status, route, call.request.resolveSensitivity())
            }
        }
    })
}

private object TmsApiMicrometerCounter {
    lateinit var config: TmsMicrometricsConfig
    fun countApiCall(statusCode: HttpStatusCode?, route: String, acr: String) {
        Counter.builder(API_CALLS_COUNTER_NAME)
            .tag("status", "${statusCode?.value ?: "NAN"}")
            .tag(
                "statusgroup",
                config.statusGroup(statusCode, route).tagName
            )
            .tag("route", route)
            .tag("acr", acr)
            .register(config.registry)
            .increment()
    }
}

class TmsMicrometricsConfig : TmsMetricsConfig() {
    private var registryWasSupplied = false
    private var performingMicrometerInstallation = false

    var installMicrometerPlugin: Boolean = false
        set(value) {
            performingMicrometerInstallation = true
            field = value
        }
    var registry: PrometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        set(value) {
            registryWasSupplied = true
            field = value
        }

    fun verify() {
        if (!registryWasSupplied) {
            when {
                !performingMicrometerInstallation && !setupMetricsRoute -> throw IllegalArgumentException("Using default registry without setting up route may cause the data to be unavaiable during scrape on applicationroute")
                performingMicrometerInstallation && !setupMetricsRoute -> throw IllegalArgumentException("Using default registry without setting up route when installing Micrometer may cause the data to be unavaiable during scrape on applicationroute")
            }
        }
    }
}




