package nav.no.tms.common.metrics


import com.auth0.jwt.JWT
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.request.*
import io.prometheus.client.Counter


val ApiResponseMetrics = createApplicationPlugin(name = "ApiResponseMetrics") {
    on(ResponseSent) { call ->
        val status = call.response.status()
        val route = call.request.uri
        ApiMetricsCounter.countApiCall(status, route, call.request.resolveSensitivity())
    }
}


private fun ApplicationRequest.resolveSensitivity(): String =
    header("token-x-authorization").acr()
        ?: authorization().acr()
        ?: "NA"


private fun String?.acr(): String? =
    this?.let {
        val jwtClaim = JWT.decode(this.split("Bearer ")[1])
            ?.getClaim("acr")
            ?.asString()
        Sensitvivity.sensitivityString(jwtClaim)
    }

private enum class Sensitvivity(val knownValues: List<String>) {
    HIGH(listOf("level4", "idporten-loa-high")), SUBSTANTIAL(listOf("level3", "idporten-loa-substantial"));

    fun contains(acrStr: String?) = knownValues.any { it == acrStr }

    companion object {
        fun sensitivityString(acrValue: String?) =
            when {
                acrValue == null -> "NA"
                HIGH.contains(acrValue) -> HIGH.name.lowercase()
                SUBSTANTIAL.contains(acrValue) -> SUBSTANTIAL.name.lowercase()
                else -> acrValue
            }
    }
}

object ApiMetricsCounter {
    const val COUNTER_NAME = "tms_api_call"
    private val counter = Counter.build()
        .name(COUNTER_NAME)
        .help("Kall til team minside sine api-er")
        .labelNames("status", "route", "statusgroup", "acr")
        .register()

    fun countApiCall(statusCode: HttpStatusCode?, route: String, acr: String) {
        counter.labels("${statusCode?.value ?: "NAN"}", route, statusCode.resolveStatusGroup(), acr).inc()
    }
}

private fun HttpStatusCode?.resolveStatusGroup() =
    when {
        this == null -> "unresolved"
        value isInStatusRange 200 -> "OK"
        value isInStatusRange 300 -> "redirection"
        value isInStatusRange (400 excluding 401 and 403) -> "client_error"
        value == 401 || value == 403 -> "auth_issues"
        value isInStatusRange 500   -> "server_error"
        else -> "unresolved"
    }

private infix fun Pair<Int, List<Int>>.and(i: Int) = this.copy(second = listOf(i) + this.second)
private infix fun Int.isInStatusRange(i: Int): Boolean = this >= i && this < (i + 100)
private infix fun Int.isInStatusRange(p: Pair<Int, List<Int>>): Boolean =
    p.second.none { it == this } && this isInStatusRange p.first

private infix fun Int.excluding(i: Int) = Pair(this, listOf(i))
