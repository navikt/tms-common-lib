package nav.no.tms.common.metrics

import com.auth0.jwt.JWT
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import nav.no.tms.common.metrics.StatusGroup.Companion.resolveStatusGroup


const val API_CALLS_COUNTER_NAME = "tms_api_call"

internal enum class Sensitivity(val knownValues: List<String>) {
    HIGH(listOf("level4", "idporten-loa-high")), SUBSTANTIAL(listOf("level3", "idporten-loa-substantial"));

    fun contains(acrStr: String?) = knownValues.any { it == acrStr }

    companion object {
        internal fun ApplicationRequest.resolveSensitivity(): String =
            header("token-x-authorization").acr()
                ?: authorization().acr()
                ?: "NA"

        private fun sensitivityString(acrValue: String?) =
            when {
                acrValue == null -> "NA"
                HIGH.contains(acrValue) -> HIGH.name.lowercase()
                SUBSTANTIAL.contains(acrValue) -> SUBSTANTIAL.name.lowercase()
                else -> acrValue
            }

        private fun String?.acr(): String? = this
            ?.split("Bearer ")
            ?.let { authHeaderArray ->
                if (authHeaderArray.size != 2) {
                    null
                } else {
                    val jwtClaim = JWT.decode(authHeaderArray[1])
                        ?.getClaim("acr")
                        ?.asString()
                    Sensitivity.sensitivityString(jwtClaim)
                }
            }
    }
}

enum class StatusGroup(val tagName: String) {
    UNRESOLVED("unresolved"), OK("OK"), REDIRECTION("redirection"),
    CLIENT_ERROR("client_error"), AUTH_ISSUES("auth_issues"), SERVER_ERROR("server_error"),
    IGNORED("ignored");

    companion object {
        internal fun HttpStatusCode?.resolveStatusGroup(): StatusGroup =
            when {
                this == null -> UNRESOLVED
                value isInStatusRange 200 -> OK
                value isInStatusRange 300 -> REDIRECTION
                value isInStatusRange (400 excluding 401 and 403) -> StatusGroup.CLIENT_ERROR
                value == 401 || value == 403 -> AUTH_ISSUES
                value isInStatusRange 500 -> SERVER_ERROR
                else -> UNRESOLVED
            }
        infix fun String.belongsTo(ok: StatusGroup) = Pair(this, ok)
        private infix fun Pair<Int, List<Int>>.and(i: Int) = this.copy(second = listOf(i) + this.second)
        private infix fun Int.isInStatusRange(i: Int): Boolean = this >= i && this < (i + 100)
        private infix fun Int.isInStatusRange(p: Pair<Int, List<Int>>): Boolean =
            p.second.none { it == this } && this isInStatusRange p.first

        private infix fun Int.excluding(i: Int) = Pair(this, listOf(i))

    }
}

open class TmsMetricsConfig {

    private val customStatusGroupMapping = mutableListOf<StatusGroupMapping>()
    private var ignoreRoutesFuction: (String, Int) -> Boolean = { _, _ -> false }
    internal val pathParamMaskingRules = mutableListOf<PathParamMask>()
    var setupMetricsRoute: Boolean = false

    internal fun statusGroup(statusCode: HttpStatusCode?, route: String): StatusGroup =
        statusCode?.let {
            customStatusGroupMapping.find { it.memberGroup(statusCode, route) != null }?.statusGroup
                ?: statusCode.resolveStatusGroup()
        } ?: StatusGroup.UNRESOLVED

    internal fun excludeRoute(route: String, status: Int?) =
        status == null ||
                route.endsWith("isready", ignoreCase = true) || route.endsWith(
            "isalive",
            ignoreCase = true
        ) || route.endsWith("metrics", ignoreCase = true) || ignoreRoutesFuction(route, status)

    fun ignoreRoutes(function: (String, Int) -> Boolean) {
        ignoreRoutesFuction = function
    }

    private val validPattern = "^((/[a-zA-Z0-9_-]+)|(/\\{[a-zA-Z0-9_-]+})|(/))+\$".toRegex()

    private val replacingPattern = "\\{[a-zA-Z0-9_-]+}".toRegex()

    fun maskPathParams(route: String) {
        if (validPattern.matches(route)) {

            val regex = replacingPattern.replace(route, "([a-zA-Z0-9_-]+)").toRegex()

            pathParamMaskingRules.add(
                PathParamMask(
                    maskedRoute = route,
                    routePattern = regex
                )
            )
        } else {
            throw IllegalArgumentException("$route is not a valid pattern.")
        }
    }

    fun statusGroups(block: () -> Unit) {
        block()
    }

    infix fun Pair<String, StatusGroup>.whenStatusIs(statusCode: HttpStatusCode) = StatusGroupMapping(
        route = first,
        statusGroup = second,
        statusCode = statusCode
    ).also { this@TmsMetricsConfig.customStatusGroupMapping.add(it) }
}

data class StatusGroupMapping(val statusCode: HttpStatusCode, val route: String, val statusGroup: StatusGroup) {

    private val comparableRoute = if (!route.startsWith("/")) "/$route" else route
    fun memberGroup(statusCode: HttpStatusCode, route: String): StatusGroup? =
        if (this.statusCode == statusCode && this.comparableRoute == route.trimMargin()) statusGroup else null
}

internal data class PathParamMask(
    val maskedRoute: String,
    val routePattern: Regex
)

internal fun recordableRoute(config: TmsMetricsConfig, request: ApplicationRequest): String {
    return applyPathParamMask(config, request.uriWithoutQuery())
}

private val uriPattern = "^([^?]+)(?:\\?.*)?\$".toRegex()

private fun ApplicationRequest.uriWithoutQuery() = uriPattern.find(uri)
    ?.destructured
    ?.component1()
    ?: uri

private fun applyPathParamMask(config: TmsMetricsConfig, route: String): String {
    return config.pathParamMaskingRules.find { it.routePattern.matches(route) }
        ?.maskedRoute
        ?: route
}
