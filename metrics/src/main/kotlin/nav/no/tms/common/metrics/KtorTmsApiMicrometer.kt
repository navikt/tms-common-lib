package nav.no.tms.common.metrics

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.Counter
import io.micrometer.prometheus.PrometheusMeterRegistry


fun Application.installTmsMicrometerMetrics(config: TmsMicrometricsConfig.() -> Unit) {
    log.info("installerer micrometer counter")
    val metricsConfig = TmsMicrometricsConfig().apply { config() }
    TmsApiMicrometerCounter.micrometerRegistry = metricsConfig.registry

    install(createApplicationPlugin(name = "TmsMicrometrics") {
        on(ResponseSent) { call ->
            val route = call.request.uri
            if (!metricsConfig.exlucdeRoute(route)) {
                val status = call.response.status()
                TmsApiMicrometerCounter.countApiCall(status, route, call.request.resolveSensitivity())
            }
        }
    })
    if (metricsConfig.setupMetricsRoute) {
        log.info("installerer route /metrics")
        routing {
            get("metrics") {
                call.respond(metricsConfig.registry.scrape())
            }
        }
    }
}

/*
fun Application.installApiMicrometerMetrics(micrometerRegistry: PrometheusMeterRegistry, withRoute: Boolean) {
    log.info("installerer micrometer counter")
    ApiMicrometerCounter.micrometerRegistry = micrometerRegistry
    install(createApplicationPlugin(name = "ApiResponseMetrics") {
        on(ResponseSent) { call ->
            val route = call.request.uri
            if (true) {
                val status = call.response.status()
                ApiMicrometerCounter.countApiCall(status, route, call.request.resolveSensitivity())
            }
        }
    })
    if (withRoute) {
        log.info("installerer route /metrics")
        routing {
            get("metrics") {
                call.respond(micrometerRegistry.scrape())
            }
        }
    }
}*/

private object TmsApiMicrometerCounter {

    lateinit var micrometerRegistry: PrometheusMeterRegistry

    fun countApiCall(statusCode: HttpStatusCode?, route: String, acr: String) {
        Counter.builder(API_CALLS_COUNTER_NAME)
            .tag("status", "${statusCode?.value ?: "NAN"}")
            .tag("statusgroup", statusCode.resolveStatusGroup())
            .tag("route", route)
            .tag("acr", acr)
            .register(micrometerRegistry)
            .increment()

        print(micrometerRegistry.meters)
    }
}

