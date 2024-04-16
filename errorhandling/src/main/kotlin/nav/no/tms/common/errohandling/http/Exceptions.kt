package nav.no.tms.common.errohandling.http

import io.ktor.client.plugins.*
import io.ktor.http.*
import nav.no.tms.common.errohandling.logging.LoggableException
import nav.no.tms.common.errohandling.redactedMessage


class ApiException(service: String, url: String, originalThrowable: Throwable, val statusCode: HttpStatusCode) :
    LoggableException(originalThrowable) {
    constructor(service: String, url: String, clientRequestException: ClientRequestException) : this(
        service,
        url,
        clientRequestException,
        clientRequestException.response.status.tmsResponseCode()
    )
    constructor(service: String, url: String, serverResponseException: ServerResponseException) : this(
        service,
        url,
        serverResponseException,
        serverResponseException.response.status.tmsResponseCode()
    )

    init {
        print(this)
    }

    override val summary: String =
        """ Feil i kall til $service med url ${url.redactedMessage(true)}
            ${stackTraceSummary(originalThrowable)}
    """.trimIndent()

    companion object {
        fun HttpStatusCode.tmsResponseCode() = when (value / 100) {
            2 -> HttpStatusCode.OK
            3 -> HttpStatusCode.InternalServerError
            4 -> HttpStatusCode.InternalServerError
            5 -> HttpStatusCode.ServiceUnavailable
            else -> HttpStatusCode.InternalServerError
        }
    }
}

