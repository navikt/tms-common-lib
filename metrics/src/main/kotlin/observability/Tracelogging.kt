package observability

import io.github.oshai.kotlinlogging.withLoggingContext

enum class Events {
    disable, enable, updated, deleted, created, oppgave, innboks, beskjed
}

enum class Contenttype {
    microfrontend, varsel, utkast
}

fun traceUtkast(
    id: String,
    event: Events,
    extra: Map<String, String> = emptyMap(),
    function: () -> Unit
) = withTraceLogging(id, event, Contenttype.utkast, extra) { function() }

fun traceMicrofrontend(
    id: String,
    event: Events,
    extra: Map<String, String> = emptyMap(),
    function: () -> Unit
) = withTraceLogging(id, event, Contenttype.microfrontend, extra) { function() }

fun traceVarsel(
    id: String,
    event: Events,
    action: String,
    extra: Map<String, String> = emptyMap(),
    function: () -> Unit
) = withTraceLogging(id, event, Contenttype.varsel, mapOf("action" to action) + extra) { function() }


fun withTraceLogging(
    id: String,
    event: Events,
    contenttype: Contenttype,
    extra: Map<String, String> = emptyMap(),
    function: () -> Unit
) {
    withLoggingContext(
        mapOf(
            "minside_id" to id,
            "event" to event.name,
            "contenttype" to contenttype.name
        ) + extra
    ) { function() }
}