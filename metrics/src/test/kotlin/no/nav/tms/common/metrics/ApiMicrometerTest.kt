package no.nav.tms.common.metrics

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusCounter
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.junit.jupiter.api.*
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class ApiMicrometricsTest {

    private val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    @Test
    fun `setter opp apiMicrometerMetrics`() =
        testApplication {
            initTestApplication(prometheusMeterRegistry = prometheusMeterRegistry)
            client.getwithAuthHeader(acr = "unknown")

            prometheusMeterRegistry.get(API_CALLS_COUNTER_NAME) shouldNotBe null
            val counterValues = prometheusMeterRegistry.meters.find { it.id.name == API_CALLS_COUNTER_NAME }.let {
                require(it != null)
                it.id
            }
            counterValues.apply {
                getTag("status") shouldBe "200"
                getTag("route") shouldBe "/test"
                getTag("statusgroup") shouldBe "OK"
                getTag("acr") shouldBe "unknown"
            }

            client.get("metrics").apply {
                status shouldBe HttpStatusCode.OK
                val body = bodyAsText()
                body shouldNotBe ""

            }
        }

    @Test
    fun `installerer med endepunkt`() = testApplication {
        application {
            installTmsMicrometerMetrics {
                setupMetricsRoute = true
            }
            install(Authentication) {
                jwt {
                    skipWhen { true }
                }
            }

            routing {
                authenticate {
                    get("test") {
                        call.respond(200)
                    }
                }
            }

        }
        client.get("test")
        client.get("test")

        client.get("metrics").apply {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldNotBe ""

        }

    }

    @Test
    fun `installerer micrometrics`() = testApplication {
        application {
            installTmsMicrometerMetrics {
                installMicrometerPlugin = true
                setupMetricsRoute = true
            }
            install(Authentication) {
                jwt {
                    skipWhen { true }
                }
            }

            routing {
                authenticate {
                    get("test") {
                        call.respond(200)
                    }
                }
            }

        }
        client.get("test")
        client.get("test")

        client.get("metrics").apply {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldNotBe ""

        }

    }

    @Test
    fun `kaster exception på dårlig config`() {
        testApplication {
            application {
                assertThrows<IllegalArgumentException> { installTmsMicrometerMetrics {} }
            }
        }
        testApplication {
            application {
                assertThrows<IllegalArgumentException> {
                    installTmsMicrometerMetrics { installMicrometerPlugin = true }
                }
            }
        }
        testApplication {
            application {
                assertDoesNotThrow {
                    installTmsMicrometerMetrics {
                        installMicrometerPlugin = true
                        setupMetricsRoute = true
                    }
                }
            }
        }
        testApplication {
            application {
                assertDoesNotThrow { installTmsApiMetrics { setupMetricsRoute = true } }
            }
        }

    }

    @Test
    fun `ignorerer path query`() = testApplication {
        initTestApplication(prometheusMeterRegistry = prometheusMeterRegistry)

        client.getwithAuthHeader(url = "/query/endpoint?param1=hello", acr = "N/A")
        client.getwithAuthHeader(url = "/query/endpoint?param2=world", acr = "N/A")
        client.getwithAuthHeader(url = "/query/endpoint?param1=hello&param2=world", acr = "N/A")

        prometheusMeterRegistry.get(API_CALLS_COUNTER_NAME) shouldNotBe null
        val counters = prometheusMeterRegistry.meters.filter { it.id.name == API_CALLS_COUNTER_NAME }

        counters.size shouldBe 1

        counters.first().apply {
            id.getTag("route") shouldBe "/query/endpoint"
            (this as PrometheusCounter).count() shouldBe 3
        }
    }

    @Test
    fun `maskerer egendefinerte path-variabler`() = testApplication {
        initTestApplication(prometheusMeterRegistry = prometheusMeterRegistry)

        client.get("/get/resource/cake/with/id/123")
        client.get("/get/resource/cookie/with/id/456")
        client.get("/get/resource/fruit/with/id/789")

        prometheusMeterRegistry.get(API_CALLS_COUNTER_NAME) shouldNotBe null
        val counters = prometheusMeterRegistry.meters.filter { it.id.name == API_CALLS_COUNTER_NAME }

        counters.first().apply {
            id.getTag("route") shouldBe "/get/resource/{name}/with/id/{id}"
            (this as PrometheusCounter).count() shouldBe 3
        }
    }

    @Test
    fun `ignorer kall med metode HEAD`() = testApplication {
        initTestApplication(prometheusMeterRegistry = prometheusMeterRegistry)

        client.head("/get/resource/cake/with/id/123")
        client.head("/get/resource/cookie/with/id/456")
        client.head("/get/resource/fruit/with/id/789")
        client.head("/does/not/exist")

        prometheusMeterRegistry.get(API_CALLS_COUNTER_NAME) shouldNotBe null
        val counters = prometheusMeterRegistry.meters.filter { it.id.name == API_CALLS_COUNTER_NAME }

        counters.size shouldBe 0
    }

}


private fun ApplicationTestBuilder.initTestApplication(
    returnStatus: HttpStatusCode = HttpStatusCode.OK,
    prometheusMeterRegistry: PrometheusMeterRegistry
) = run {
    application {
        install(MicrometerMetrics) {
            registry = prometheusMeterRegistry
        }
        installTmsMicrometerMetrics {
            registry = prometheusMeterRegistry
            setupMetricsRoute = true
        }

    }
    install(Authentication) {
        jwt {
            skipWhen { true }
        }
    }

    routing {
        authenticate {
            get("test") {
                call.respond(returnStatus)
            }
            get("/get/resource/{name}/with/id/{id}") {
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}

private suspend fun HttpClient.getwithAuthHeader(
    url: String = "test",
    acr: String,
    authHeaderName: String = "Authorization"
) {
    get(url) {
        headers {
            header(authHeaderName, "Bearer ${generateToken(acr)}")
        }
    }
}

private fun generateToken(
    acr: String,
    audience: String = "default",
    issuer: String = "default",
    secret: String = UUID.randomUUID().toString()
) = JWT.create()
    .withAudience(audience)
    .withIssuer(issuer)
    .withClaim("acr", acr)
    .sign(Algorithm.HMAC256(secret))