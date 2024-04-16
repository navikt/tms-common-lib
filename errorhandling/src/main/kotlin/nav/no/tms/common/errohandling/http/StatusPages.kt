package nav.no.tms.common.errohandling.nav.no.tms.common.errohandling.http

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import nav.no.tms.common.errohandling.http.ServiceRequestException
import nav.no.tms.common.errohandling.logging.LoggableException
import nav.no.tms.common.errohandling.logging.TmsLog
import nav.no.tms.common.errohandling.logging.TmsSecureLog
import nav.no.tms.common.errohandling.redactedMessage

fun Application.installTmsStatusPages(secureLog: TmsSecureLog, log: TmsLog) {
    install(StatusPages) {
        exception<Throwable> { call, cause ->

            when (cause) {
                is ServiceRequestException -> {
                    secureLog.error(cause)
                    log.error(cause)
                    call.respond(cause.statusCode)
                }

                is LoggableException -> {
                    secureLog.error(cause)
                    log.error(cause)
                    call.respond(HttpStatusCode.InternalServerError)
                }

                else -> {
                    secureLog.error(cause){ "Ukjent feil i kall til ${call.request.uri.redactedMessage(true)}" }
                }
            }
        }
    }
}