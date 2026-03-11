package no.nav.tms.common.observability

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import no.nav.tms.common.observability.Domain.Companion.none
import no.nav.tms.common.observability.Domain.Companion.varsel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.MDC


private val logger = KotlinLogging.logger { }

class ApiMdcTest {
    private val objectMapper = jacksonObjectMapper()

    @BeforeEach
    fun setUp() {
        MDC.clear()
    }

    @AfterEach
    fun tearDown() {
        MDC.clear()
    }

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
        mdcTestApplication(
            extraRoutes = { post("custom") { call.respond(MDC.getCopyOfContextMap() ?: emptyMap<String, String>()) } },
            mdcConfig = { this.applicationDomain = varsel }) {
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
    fun `legger til domene på routes og metodenivå `() = mdcTestApplication(
        mdcConfig = {
            applicationDomain = varsel
        },
        extraRoutes = {
            route("route") {
                mdcDomain = Domain.custom("route")
                post {
                    call.respond(MDC.getCopyOfContextMap() ?: emptyMap<String, String>())
                }
                get("method") {
                    call.mdcDomain = Domain.custom("method")
                    call.respond(MDC.getCopyOfContextMap() ?: emptyMap<String, String>())
                }
            }
            get("nodomain") {
                call.mdcDomain = none
                call.respond(MDC.getCopyOfContextMap() ?: emptyMap<String, String>())
            }
        }
    ) {
        val getMdcVarsel = client.get("varsel").mdcMap()
        getMdcVarsel["route"].asText() shouldBe "/varsel"
        getMdcVarsel["method"].asText() shouldBe "GET"
        getMdcVarsel["domain"].asText() shouldBe "varsel"

        val postMdcVarsel = client.post("varsel").mdcMap()
        postMdcVarsel["route"].asText() shouldBe "/varsel"
        postMdcVarsel["method"].asText() shouldBe "POST"
        postMdcVarsel["domain"].asText() shouldBe "varsel"

        val postRouteMdc = client.post("route").mdcMap()
        postRouteMdc["route"].asText() shouldBe "/route"
        postRouteMdc["method"].asText() shouldBe "POST"
        postRouteMdc["domain"].asText() shouldBe "route"

        val getMethodMdc = client.get("route/method").mdcMap()
        getMethodMdc["route"].asText() shouldBe "/route/method"
        getMethodMdc["method"].asText() shouldBe "GET"
        getMethodMdc["domain"].asText() shouldBe "method"

        val nodomainMdc = client.get("nodomain").mdcMap()
        nodomainMdc["route"].asText() shouldBe "/nodomain"
        nodomainMdc["method"].asText() shouldBe "GET"
        nodomainMdc.has("domain") shouldBe false
    }

    @Test
    fun `beholder MDC-verdier når exception kastes`() = mdcTestApplication(
        mdcConfig = { applicationDomain = varsel },
        extraRoutes = {
            get("exception") {
                throw IllegalStateException("test exception")
            }
        }
    ) {
        val mdc = client.get("exception").mdcMap(HttpStatusCode.InternalServerError)
        mdc["route"].asText() shouldBe "/exception"
        mdc["method"].asText() shouldBe "GET"
        mdc["domain"].asText() shouldBe "varsel"
    }


    private suspend fun HttpResponse.mdcMap(expectStatus: HttpStatusCode = HttpStatusCode.OK): JsonNode = this.let {
        status shouldBe expectStatus
        objectMapper.readTree(bodyAsText())
    }
}


fun mdcTestApplication(
    extraRoutes: Route.() -> Unit = {},
    mdcConfig: MdcDomainConfig.() -> Unit, block: suspend ApplicationTestBuilder.() -> Unit
) =
    testApplication {

        install(MdcContextLogger)
        install(ApiMdc) {
            mdcConfig()
        }
        install(ContentNegotiation) {
            jackson {}
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                call.respond(
                    HttpStatusCode.InternalServerError,
                    MDC.getCopyOfContextMap() ?: emptyMap<String, String>()
                )
            }
        }
        routing {
            route("varsel") {
                get {
                    call.respond(MDC.getCopyOfContextMap() ?: emptyMap<String, String>())
                }
                post {
                    call.respond(MDC.getCopyOfContextMap() ?: emptyMap<String, String>())
                }
            }
            extraRoutes()
        }
        block(this)
    }


fun assertThrowsForReason(reason: String, block: () -> Unit) {
    withClue("$reason fører ikke til IllegalArgumentException") {
        assertThrows<IllegalArgumentException> { block() }
    }

}

val MdcContextLogger = createApplicationPlugin(name = "MdcContextLogger") {
    val logger = KotlinLogging.logger { }
    var loggingJob: Job? = null

    on(MonitoringEvent(ApplicationStarted)) { application ->
        loggingJob = application.launch {
            while (isActive) {
                val mdcMap = MDC.getCopyOfContextMap() ?: emptyMap<String, String>()
                if (mdcMap.containsKey("route")) {
                    logger.error { "Mdc present outside of call" }
                    throw AssertionError("Context blead from API-mdc in route "+mdcMap["route"])
                } else {
                    logger.info { "Mdc preserved within call to route" }
                }
                delay(1)
            }
        }
    }

    on(MonitoringEvent(ApplicationStopped)) { _ ->
        loggingJob?.cancel()
        loggingJob = null
    }
}
