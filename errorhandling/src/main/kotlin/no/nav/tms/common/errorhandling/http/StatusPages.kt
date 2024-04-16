package no.nav.tms.common.errorhandling.http

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import no.nav.tms.common.errorhandling.logging.LoggableException
import no.nav.tms.common.errorhandling.logging.TmsLog
import no.nav.tms.common.errorhandling.logging.TmsSecureLog
import no.nav.tms.common.errorhandling.redactedMessage


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