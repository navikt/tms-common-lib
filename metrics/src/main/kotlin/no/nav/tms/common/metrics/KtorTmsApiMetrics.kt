package no.nav.tms.common.metrics


import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.prometheus.metrics.core.metrics.Counter
import io.prometheus.metrics.expositionformats.ExpositionFormats
import io.prometheus.metrics.model.registry.MetricNameFilter
import io.prometheus.metrics.model.registry.PrometheusRegistry


fun Application.installTmsApiMetrics(config: TmsMetricsConfig.() -> Unit) {

    val metricsConfig = TmsMetricsConfig().apply(config)

    TmsApiMetricsCounter.config = metricsConfig

    log.info("Installerer api metrics")
    installMetrics(metricsConfig, TmsApiMetricsCounter)

    if (metricsConfig.setupMetricsRoute) {

        log.info("installerer endepunkt /metrics med defaultregistry")

        routing {
            val writer = ExpositionFormats.init().openMetricsTextFormatWriter

            get("/metrics") {
                PrometheusRegistry.defaultRegistry.scrape()

                val requestedNames = call.request.queryParameters.getAll("name[]")?.toSet() ?: emptySet()

                val filter = if (requestedNames.isNotEmpty()) {
                    MetricNameFilter.builder().nameMustBeEqualTo(requestedNames).build()
                } else {
                    null
                }

                call.respondOutputStream(ContentType.parse(writer.contentType)) {
                    writer.write(this, PrometheusRegistry.defaultRegistry.scrape(filter))
                }
            }
        }
    }
}

private object TmsApiMetricsCounter: Reporter {
    lateinit var config: TmsMetricsConfig

    private val counter = Counter.builder()
        .name(API_CALLS_COUNTER_NAME)
        .help("Kall til team minside sine api-er")
        .labelNames("status", "route", "statusgroup", "acr")
        .register()

    override fun countApiCall(statusCode: HttpStatusCode?, route: String, acr: String) {
        counter.labelValues("${statusCode?.value ?: "NAN"}", route, config.statusGroup(statusCode, route).tagName, acr)
            .inc()
    }
}

