package nav.no.tms.common.metrics

import com.auth0.jwt.JWT
import io.ktor.http.*
import io.ktor.server.request.*

const val API_CALLS_COUNTER_NAME = "tms_api_call"

internal fun HttpStatusCode?.resolveStatusGroup() =
    when {
        this == null -> "unresolved"
        value isInStatusRange 200 -> "OK"
        value isInStatusRange 300 -> "redirection"
        value isInStatusRange (400 excluding 401 and 403) -> "client_error"
        value == 401 || value == 403 -> "auth_issues"
        value isInStatusRange 500 -> "server_error"
        else -> "unresolved"
    }

internal infix fun Pair<Int, List<Int>>.and(i: Int) = this.copy(second = listOf(i) + this.second)
internal infix fun Int.isInStatusRange(i: Int): Boolean = this >= i && this < (i + 100)
internal infix fun Int.isInStatusRange(p: Pair<Int, List<Int>>): Boolean =
    p.second.none { it == this } && this isInStatusRange p.first

internal infix fun Int.excluding(i: Int) = Pair(this, listOf(i))

internal fun ApplicationRequest.resolveSensitivity(): String =
    header("token-x-authorization").acr()
        ?: authorization().acr()
        ?: "NA"

internal fun String?.acr(): String? = this
    ?.split("Bearer ")
    ?.let { authHeaderArray ->
        if(authHeaderArray.size !=2){null}
        else {
            val jwtClaim = JWT.decode(authHeaderArray[1])
                ?.getClaim("acr")
                ?.asString()
            Sensitivity.sensitivityString(jwtClaim)
        }
    }

internal enum class Sensitivity(val knownValues: List<String>) {
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
