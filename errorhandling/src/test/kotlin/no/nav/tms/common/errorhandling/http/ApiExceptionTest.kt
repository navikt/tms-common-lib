package no.nav.tms.common.errorhandling.http

import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.ServiceUnavailable
import io.ktor.http.HttpStatusCode.Companion.TemporaryRedirect
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import no.nav.tms.common.errorhandling.http.ServiceRequestException.Companion.tmsResponseCode
import org.junit.jupiter.api.Test

class ApiExceptionTest {

    @Test
    fun `riktig responskode`() {
        InternalServerError.tmsResponseCode() shouldBe ServiceUnavailable
        ServiceUnavailable.tmsResponseCode() shouldBe ServiceUnavailable
        BadRequest.tmsResponseCode() shouldBe InternalServerError
        Unauthorized.tmsResponseCode() shouldBe InternalServerError
        TemporaryRedirect.tmsResponseCode() shouldBe InternalServerError
    }
}