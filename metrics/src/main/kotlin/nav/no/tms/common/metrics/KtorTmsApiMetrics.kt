package nav.no.tms.common.metrics


import io.ktor.http.*
import io.ktor.server.application.*

import io.ktor.server.application.hooks.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter
import io.prometheus.client.exporter.common.TextFormat
import nav.no.tms.common.metrics.Sensitivity.Companion.resolveSensitivity


fun Application.installTmsApiMetrics(config: TmsMetricsConfig.() -> Unit) {
    val metricsConfig = TmsMetricsConfig().apply(config)
    TmsApiMetricsCounter.config = metricsConfig
    log.info("Installerer api metrics")
    install(createApplicationPlugin(name = "ApiResponseMetrics") {
        on(ResponseSent) { call ->
            val route = call.request.uri
            val status = call.response.status()
            if (!metricsConfig.excludeRoute(route, status?.value)) {
                TmsApiMetricsCounter.countApiCall(status, route, call.request.resolveSensitivity())
            }

        }
    })
    if (metricsConfig.setupMetricsRoute) {
        log.info("installerer endepunkt /metrics med defaultregistry")
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

private object TmsApiMetricsCounter {
    lateinit var config: TmsMetricsConfig

    private val counter = Counter.build()
        .name(API_CALLS_COUNTER_NAME)
        .help("Kall til team minside sine api-er")
        .labelNames("status", "route", "statusgroup", "acr")
        .register()

    fun countApiCall(statusCode: HttpStatusCode?, route: String, acr: String) {
        counter.labels("${statusCode?.value ?: "NAN"}", route, config.statusGroup(statusCode,route).tagName, acr)
            .inc()
    }
}

