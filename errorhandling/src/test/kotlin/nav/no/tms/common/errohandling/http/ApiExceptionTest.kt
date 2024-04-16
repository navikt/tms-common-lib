package nav.no.tms.common.errohandling.http

import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.ServiceUnavailable
import io.ktor.http.HttpStatusCode.Companion.TemporaryRedirect
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import nav.no.tms.common.errohandling.http.ApiException.Companion.tmsResponseCode
import org.junit.jupiter.api.Test

class ApiExceptionTest {
    private val dummyservice = "dummy"
    private val dummyUrl = "dummy"

    @Test
    fun `riktig responskode`() {
        InternalServerError.tmsResponseCode() shouldBe ServiceUnavailable
        ServiceUnavailable.tmsResponseCode() shouldBe ServiceUnavailable
        BadRequest.tmsResponseCode() shouldBe InternalServerError
        Unauthorized.tmsResponseCode() shouldBe InternalServerError
        TemporaryRedirect.tmsResponseCode() shouldBe InternalServerError
    }
}