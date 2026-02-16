package no.nav.tms.common.observability

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.tms.common.observability.Domain.Companion.mixed
import no.nav.tms.common.observability.Domain.Companion.varsel
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.MDC


class TraceloggingTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `Kaster feil ved ugyldig domene navn`() {
        assertThrowsForReason("predefinert verdi") { Domain.custom("utkast-krt") }
        assertThrowsForReason("predefinert verdi med stor bokstav") { Domain.custom("Varsel") }
        assertThrowsForReason("feilstavet predefinert verdi") { Domain.custom("mikrofrontend") }
        assertThrowsForReason("ikke tillat bokstav å") { Domain.custom("åring") }
        assertThrowsForReason("For kort") { Domain.custom("ing") }
        assertThrowsForReason("ugyldig vhar /") { Domain.custom("ing/jg") }
        assertThrowsForReason("inneholder tall") { Domain.custom("ing88") }
        assertThrowsForReason("For langt") { Domain.custom("ingnhhkdsa-sdhgjsdhgjksddjkaasd") }
        assertDoesNotThrow { Domain.custom("utbetaling") }
        assertDoesNotThrow { Domain.custom("proxy-api") }
    }

    @Test
    fun `legger til route og method MDC med domene for hele applikasjonen`() =
        mdcTestApplication({ this.applicationDomain = varsel }) {
            val getMdc = client.get("varsel").mdcMap()
            getMdc["route"].asText() shouldBe "/varsel"
            getMdc["method"].asText() shouldBe "GET"
            getMdc["domain"].asText() shouldBe "varsel"

            val postMdc = client.post("custom").mdcMap()
            postMdc["route"].asText() shouldBe "/custom"
            postMdc["method"].asText() shouldBe "POST"
            postMdc["domain"].asText() shouldBe "varsel"
        }

    @Test
    fun `legger til domene på routes og metodenivå `() = mdcTestApplication({
        applicationDomain = mixed
        routeScopedDomains = true
        methodScopedDomains = true
    }) {
        val getMdcVarsel = client.get("varsel").mdcMap()
        getMdcVarsel["route"].asText() shouldBe "/varsel"
        getMdcVarsel["method"].asText() shouldBe "GET"
        getMdcVarsel["domain"].asText() shouldBe "varsel"
        val postMdcVarsel = client.post("varsel").mdcMap()
        postMdcVarsel["route"].asText() shouldBe "/varsel"
        postMdcVarsel["method"].asText() shouldBe "POST"
        postMdcVarsel["domain"].asText() shouldBe "something-else"

        val postMdc = client.post("custom").mdcMap()
        postMdc["route"].asText() shouldBe "/custom"
        postMdc["method"].asText() shouldBe "POST"
        postMdc["domain"].asText() shouldBe "custom"

        val nodomainMdc = client.get("nodomain").mdcMap()
        nodomainMdc["route"].asText() shouldBe "/nodomain"
        nodomainMdc["method"].asText() shouldBe "GET"
        nodomainMdc.has("domain") shouldBe false
    }

    private suspend fun HttpResponse.mdcMap(): JsonNode = this.let {
        status shouldBe HttpStatusCode.OK
        objectMapper.readTree(bodyAsText())
    }
}


fun mdcTestApplication(mdcConfig: MdcDomainConfig.() -> Unit, block: suspend ApplicationTestBuilder.() -> Unit) =
    testApplication {
        install(ApiMdc) {
            mdcConfig()
        }
        install(ContentNegotiation) {
            jackson {}
        }
        routing {
            route("varsel") {
                mdcDomain = Domain.varsel
                get {
                    call.respond(MDC.getCopyOfContextMap())
                }
                post {
                    call.mdcDomain = Domain.custom("something-else")
                    call.respond(MDC.getCopyOfContextMap())
                }
            }
            route("custom") {
                mdcDomain = Domain.custom("custom")
                post {
                    call.respond(MDC.getCopyOfContextMap())
                }
            }
            get("nodomain") {
                call.respond(MDC.getCopyOfContextMap())
            }
        }

        block(this)
    }


fun assertThrowsForReason(reason: String, block: () -> Unit) {
    withClue("$reason fører ikke til IllegalArgumentException") {
        assertThrows<IllegalArgumentException> { block() }
    }
}
