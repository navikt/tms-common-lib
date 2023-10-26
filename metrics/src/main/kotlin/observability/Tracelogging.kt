package observability

import io.github.oshai.kotlinlogging.withLoggingContext

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