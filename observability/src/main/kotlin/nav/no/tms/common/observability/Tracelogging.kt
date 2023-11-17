package observability

import io.github.oshai.kotlinlogging.withLoggingContext
import io.ktor.serialization.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.util.*
import org.slf4j.MDC

enum class Contenttype {
    microfrontend, varsel, utkast, utbetaling, dokumenter
}

fun traceUtkast(
    id: String,
    extra: Map<String, String> = emptyMap(),
    function: () -> Unit
) = withTraceLogging(id, Contenttype.utkast, extra) { function() }

fun traceMicrofrontend(
    id: String,
    extra: Map<String, String> = emptyMap(),
    function: () -> Unit
) = withTraceLogging(id, Contenttype.microfrontend, extra) { function() }

fun traceVarsel(
    id: String,
    extra: Map<String, String> = emptyMap(),
    function: () -> Unit
) = withTraceLogging(id, Contenttype.varsel, extra) { function() }


fun withTraceLogging(
    id: String,
    contenttype: Contenttype,
    extra: Map<String, String> = emptyMap(),
    function: () -> Unit
) {
    withLoggingContext(
        mapOf(
            "minside_id" to id,
            "contenttype" to contenttype.name
        ) + extra
    ) { function() }
}

suspend fun withTraceLoggingAsync(
    id: String,
    contenttype: Contenttype,
    extra: Map<String, String> = emptyMap(),
    function: suspend () -> Unit
) {
    withLoggingContext(
        mapOf(
            "minside_id" to id,
            "contenttype" to contenttype.name
        ) + extra
    ) { function() }
}

suspend fun withApiTracing(
    route: String,
    contenttype: Contenttype,
    extra: Map<String, String> = emptyMap(),
    method: String = "GET",
    function: suspend () -> Unit
) {
    withLoggingContext(
        mapOf(
            "route" to "$method – $route",
            "contenttype" to contenttype.name
        ) + extra
    ) { function() }
}

class ApiMdc{
    companion object Plugin : BaseApplicationPlugin<ApplicationCallPipeline, Configuration, ApiMdc> {

        override val key = AttributeKey<ApiMdc>("ApiMdc")
        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): ApiMdc {
            val plugin = ApiMdc()
            pipeline.intercept(ApplicationCallPipeline.Monitoring) {
                MDC.put("route", call.request.uri)
            }
            return plugin
        }
    }
}
