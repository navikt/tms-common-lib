package nav.no.tms.common.metrics

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.Counter
import io.micrometer.prometheus.PrometheusMeterRegistry


fun Application.installApiMicrometer(micrometerRegistry: PrometheusMeterRegistry, withRoute: Boolean) {
    log.info("installerer micrometer counter")
    ApiMicometerCounter.micrometerRegistry = micrometerRegistry
    install(createApplicationPlugin(name = "ApiResponseMetrics") {
        on(ResponseSent) { call ->
            val status = call.response.status()
            val route = call.request.uri
            ApiMicometerCounter.countApiCall(status, route, call.request.resolveSensitivity())
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
}

private object ApiMicometerCounter {

    lateinit var micrometerRegistry: PrometheusMeterRegistry

    fun countApiCall(statusCode: HttpStatusCode?, route: String, acr: String) {
        Counter.builder(COUNTER_NAME)
            .tag("status","${statusCode?.value ?: "NAN"}")
            .tag("statusgroup", statusCode.resolveStatusGroup())
            .tag("route", route)
            .tag("acr", acr)
            .register(micrometerRegistry)
            .increment()

        print(micrometerRegistry.meters)
    }
}

