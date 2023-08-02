package nav.no.tms.common.metrics


import com.auth0.jwt.JWT
import io.ktor.http.*
import io.ktor.server.application.*

import io.ktor.server.application.hooks.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter
import io.prometheus.client.exporter.common.TextFormat


val ApiResponseMetrics = createApplicationPlugin(name = "ApiResponseMetrics") {
    on(ResponseSent) { call ->
        val status = call.response.status()
        val route = call.request.uri
        ApiMetricsCounter.countApiCall(status, route, call.request.resolveSensitivity())
    }
}

fun Application.installApiMetrics(withRoute: Boolean) {
    log.info("installerer apimetrics")
    install(ApiResponseMetrics)
    if (withRoute) {
        log.info("installerer endepunkt /metrics")
        routing {
            get("/metrics") {
                val collectorRegistry = CollectorRegistry.defaultRegistry
                call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
                    TextFormat.write004(this, collectorRegistry.filteredMetricFamilySamples(emptySet()))
                }
            }
        }
    }
}

private object ApiMetricsCounter {
    private val counter = Counter.build()
        .name(COUNTER_NAME)
        .help("Kall til team minside sine api-er")
        .labelNames("status", "route", "statusgroup", "acr")
        .register()

    fun countApiCall(statusCode: HttpStatusCode?, route: String, acr: String) {
        counter.labels("${statusCode?.value ?: "NAN"}", route, statusCode.resolveStatusGroup(), acr).inc()
    }
}

