package nav.no.tms.common.metrics

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import nav.no.tms.common.metrics.Sensitivity.Companion.resolveSensitivity

internal fun Application.installMetrics(metricsConfig: TmsMetricsConfig, reporter: Reporter) {
    log.info("Installerer TmsApiMicrometrics for Ktor")

    install(createApplicationPlugin(name = "TmsMicrometrics") {

        on(MonitoringEvent(Routing.RoutingCallStarted)) {
            it.attributes.put(TmsMetricsConfig.routeKey, it.routeStr())
        }

        on(ResponseSent) { call ->
            val route = call.attributes.getOrNull(TmsMetricsConfig.routeKey) ?: call.request.uriWithoutQuery()
            val status = call.response.status()

            if (!metricsConfig.excludeRoute(route, status?.value)) {
                reporter.countApiCall(status, route, call.request.resolveSensitivity())
            }
        }
    })
}

internal interface Reporter {
    fun countApiCall(statusCode: HttpStatusCode?, route: String, acr: String)
}
private fun ApplicationRequest.uriWithoutQuery() =
    "^([^?]+)(?:\\?.*)?\$".toRegex()
        .find(uri)
        ?.destructured
        ?.component1()
        ?: uri