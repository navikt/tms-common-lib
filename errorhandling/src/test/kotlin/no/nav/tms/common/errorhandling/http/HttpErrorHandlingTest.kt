package no.nav.tms.common.errorhandling.http

import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.tms.common.errorhandling.http.TestApplication.testApp
import no.nav.tms.common.errorhandling.http.TestApplication.badRequestRoute
import no.nav.tms.common.errorhandling.http.TestApplication.okRoute
import no.nav.tms.common.errorhandling.http.TestApplication.redirectRoute
import no.nav.tms.common.errorhandling.http.TestApplication.serverErrorRoute
import org.junit.jupiter.api.Test

class HttpErrorHandlingTest {

    @Test
    fun getFromService() {
        testApplication {
            testApp()
            client.get(badRequestRoute).status shouldBe HttpStatusCode.InternalServerError
            client.get(redirectRoute).status shouldBe HttpStatusCode.InternalServerError
            client.get(serverErrorRoute).status shouldBe HttpStatusCode.ServiceUnavailable
            client.get(okRoute).status shouldBe HttpStatusCode.OK
        }
    }
}
