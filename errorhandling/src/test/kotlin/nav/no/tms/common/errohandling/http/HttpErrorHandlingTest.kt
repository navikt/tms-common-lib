package nav.no.tms.common.errohandling.http

import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import nav.no.tms.common.errohandling.http.TestApplication.app
import nav.no.tms.common.errohandling.http.TestApplication.badRequestRoute
import nav.no.tms.common.errohandling.http.TestApplication.okRoute
import nav.no.tms.common.errohandling.http.TestApplication.redirectRoute
import nav.no.tms.common.errohandling.http.TestApplication.serverErrorRoute
import nav.no.tms.common.errohandling.http.TestApplication.timeoutRoute
import nav.no.tms.common.testutils.RouteProvider
import nav.no.tms.common.testutils.initExternalServices
import org.junit.jupiter.api.Test
import java.lang.Thread.sleep

class HttpErrorHandlingTest {

    @Test
    fun getFromService() {
        testApplication {
            app()
            client.get(badRequestRoute).status shouldBe HttpStatusCode.InternalServerError
            client.get(redirectRoute).status shouldBe HttpStatusCode.InternalServerError
            client.get(serverErrorRoute).status shouldBe HttpStatusCode.ServiceUnavailable
            client.get(okRoute).status shouldBe HttpStatusCode.OK
            //client.get(timeoutRoute).status shouldBe HttpStatusCode.ServiceUnavailable TODO: teste  timeout
        }
    }
}
