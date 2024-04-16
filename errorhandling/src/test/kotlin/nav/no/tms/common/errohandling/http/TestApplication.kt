package nav.no.tms.common.errohandling.http

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import nav.no.tms.common.errohandling.logging.TmsLog
import nav.no.tms.common.errohandling.logging.TmsSecureLog
import nav.no.tms.common.errohandling.nav.no.tms.common.errohandling.http.installTmsStatusPages
import nav.no.tms.common.testutils.RouteProvider
import nav.no.tms.common.testutils.initExternalServices

object TestApplication {
    const val serverErrorRoute = "servererror"
    const val badRequestRoute = "badrequest"
    const val redirectRoute = "redirect"
    const val okRoute = "ok"
    const val timeoutRoute = "timeout"
    const val testHost = "http://test.nav.no"
    private fun Application.routes(client: HttpClient) {
        routing {
            get(serverErrorRoute) {
                call.respond(
                    client.getFromService(
                        "$testHost/$serverErrorRoute",
                        "SAF"
                    ).status
                )
            }
            get(badRequestRoute) {
                call.respond(
                    client.getFromService(
                        "$testHost/$badRequestRoute",
                        "SAF"
                    ).status
                )
            }
            get(redirectRoute) {
                call.respond(
                    client.getFromService(
                        "$testHost/$redirectRoute",
                        "SAF"
                    ).status
                )
            }
            get(okRoute) {
                call.respond(
                    client.getFromService(
                        "$testHost/$okRoute",
                        "SAF"
                    ).status
                )
            }
            get(timeoutRoute) {
                call.respond(
                    client.getFromService(
                        "$testHost/$okRoute",
                        "SAF"
                    ).status
                )
            }
        }
    }

    fun ApplicationTestBuilder.app() {
        val httpClient = createClient {
            install(HttpTimeout) {
                this.requestTimeoutMillis = 500
            }
        }

        val tmsLog = TmsLog.getLog {  }
        val secureLog = TmsSecureLog.getSecureLog()

        application {
            installTmsStatusPages(secureLog,tmsLog)
            routes(httpClient)
        }

        initExternalServices(
            testHost,
            GetTestRouteProvider(HttpStatusCode.InternalServerError, serverErrorRoute),
            GetTestRouteProvider(HttpStatusCode.BadRequest, badRequestRoute),
            GetTestRouteProvider(HttpStatusCode.PermanentRedirect, redirectRoute),
            GetTestRouteProvider(HttpStatusCode.OK, okRoute),
            GetTestRouteProvider(HttpStatusCode.OK, timeoutRoute) {
                Thread.sleep(1500)
            }
        )
    }
}

class GetTestRouteProvider(
    statusCode: HttpStatusCode, path: String, assert: suspend (ApplicationCall) -> Unit = {}
) :
    RouteProvider(path, statusCode = statusCode, routeMethodFunction = Routing::get, assert = assert) {
    override fun content(): String = "Response response"
}