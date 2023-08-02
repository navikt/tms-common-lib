package nav.no.tms.common.metrics

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
import io.micrometer.prometheus.*
import io.prometheus.client.Collector
import io.prometheus.client.CollectorRegistry
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class ApiMicrometricsTest {

    private val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    @Test
    fun `setter opp apiMicrometrics`() =
        testApplication {
            initTestApplication(prometheusMeterRegistry = prometheusMeterRegistry)
            client.getwithAuthHeader(acr = "unknown")

            prometheusMeterRegistry.get(COUNTER_NAME) shouldNotBe null
            val counterValues = prometheusMeterRegistry.meters.find { it.id.name == COUNTER_NAME }.let {
                require(it != null)
                it.id
            }
            counterValues.apply {
                getTag("status") shouldBe "200"
                getTag("route") shouldBe "/test"
                getTag("statusgroup") shouldBe "OK"
                getTag("acr") shouldBe "unknown"
            }

            client.get("metrics").apply{
                status shouldBe HttpStatusCode.OK
                val body = bodyAsText()
                body shouldNotBe ""

            }
        }
    @Test
    fun `installerer med endepunkt`() = testApplication {
        application {
            installApiMetrics(true)
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

}

private fun CollectorRegistry.assertCounterValue(
    counterValue: Int,
    function: Collector.MetricFamilySamples.Sample.() -> Boolean
) {
    metricFamilySamples().asIterator().next().samples.find { it.function() }.apply {
        require(this != null)
        value shouldBe counterValue
    }
}


private fun ApplicationTestBuilder.initTestApplication(
    returnStatus: HttpStatusCode = HttpStatusCode.OK,
    prometheusMeterRegistry: PrometheusMeterRegistry
) = run {
    application {

        install(MicrometerMetrics) {
            registry = prometheusMeterRegistry
            // ...
        }
        installApiMicrometer(prometheusMeterRegistry, withRoute = true)
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

private fun allRoutes(root: Route): List<Route> {
    return listOf(root) + root.children.flatMap { allRoutes(it) }
}