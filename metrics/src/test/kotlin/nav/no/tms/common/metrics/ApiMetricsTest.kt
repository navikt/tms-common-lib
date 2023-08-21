package nav.no.tms.common.metrics

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.collections.shouldNotContainInOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.prometheus.client.Collector.MetricFamilySamples.Sample
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.CollectorRegistry.defaultRegistry
import nav.no.tms.common.metrics.StatusGroup.Companion.belongsTo
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ApiMetricsTest {

    @Test
    @Order(1)
    fun `setter opp apiMetrics plugin`() =
        testApplication {
            initTestApplication()
            client.getwithAuthHeader(acr = "unknown")

            val collected =
                defaultRegistry.metricFamilySamples().asIterator().next().samples.first()
            require(collected != null)
            collected.apply {
                labelNames[0] shouldBe "status"
                labelValues[0] shouldBe "200"
                labelNames[1] shouldBe "route"
                labelValues[1] shouldBe "/test"
                labelNames[2] shouldBe "statusgroup"
                labelValues[2] shouldBe "OK"
                labelNames[3] shouldBe "acr"
                labelValues[3] shouldBe "unknown"
            }
        }

    @Test
    @Order(2)
    fun `henter ut sensitivitet`() =
        testApplication {
            initTestApplication()

            client.getwithAuthHeader(acr = "level4")
            defaultRegistry.assertCounterValue(1) { labelValues[3] == "high" }

            client.getwithAuthHeader(acr = "idporten-loa-high")
            defaultRegistry.assertCounterValue(2) { labelValues[3] == "high" }

            client.getwithAuthHeader(acr = "idporten-loa-substantial")
            defaultRegistry.assertCounterValue(1) { labelValues[3] == "substantial" }

            client.getwithAuthHeader(acr = "level3")
            defaultRegistry.assertCounterValue(2) { labelValues[3] == "substantial" }

            client.get("/test")
            defaultRegistry.assertCounterValue(1) { labelValues[3] == "NA" }

        }

    @ParameterizedTest
    @Order(3)
    @EnumSource(TestStatusCode::class)
    fun `mapper status riktig`(code: TestStatusCode) = testApplication {
        initTestApplication(code.returnStatus)
        client.getwithAuthHeader(acr = "something")

        val sample = defaultRegistry.metricFamilySamples().asIterator().next().samples.find {
            it.labelValues[0] == code.expectedStatusString
        }

        require(sample != null)
        sample.labelValues[2] shouldBe code.expectedStatusGroup

    }

    @Test
    @Order(4)
    fun `installerer med endpunkt`() = testApplication {
        application {
            installTmsApiMetrics { setupMetricsRoute = true }
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
    @Order(5)
    fun `ignorerer ruter med gitt status`() {

        testApplication {
            application {
                installTmsApiMetrics {
                    ignoreRoutes { route, status ->
                        (route == "/ignore" && status == 201) || (route == "dontignoreforthisstatus" && status == 500)
                    }
                }
            }

            routing {
                get("ignore") {
                    call.respond(HttpStatusCode.Created)
                }
                get("dontignore") {
                    call.respond(HttpStatusCode.Created)
                }
                get("dontignoreforthisstatus") {
                    call.respond(HttpStatusCode.Created)
                }
            }

            client.get("ignore")
            client.get("dontignore")
            client.get("dontignoreforthisstatus")

            defaultRegistry.assertCounterValue(0) { labelValues[1] == "/ignore" }
            defaultRegistry.assertCounterValue(1) { labelValues[1] == "/dontignore" }
            defaultRegistry.assertCounterValue(1) { labelValues[1] == "/dontignoreforthisstatus" }
        }
    }

    @Test
    @Order(6)
    fun `custommapping for statusgruppe`() {
        val requestIterator =
            listOf(
                HttpStatusCode.BadRequest,
                HttpStatusCode.Unauthorized,
                HttpStatusCode.OK,
                HttpStatusCode.OK
            ).iterator()

        testApplication {
            application {
                installTmsApiMetrics {
                    statusGroups {
                        "map" belongsTo StatusGroup.IGNORED whenStatusIs HttpStatusCode.BadRequest
                        "map" belongsTo StatusGroup.SERVER_ERROR whenStatusIs HttpStatusCode.Unauthorized
                    }
                }
            }

            routing {
                get("map") {
                    call.respond(requestIterator.next())
                }
            }

            client.get("map")
            client.get("map")
            client.get("map")
            client.get("map")

            val metrics = defaultRegistry.metricFamilySamples()
                .nextElement().samples.filter { it.name == "tms_api_call_total" && it.labelValues[1] == "/map" }

            metrics.find { it.labelValues[2] == StatusGroup.OK.tagName }?.value shouldBe 2
            metrics.find { it.labelValues[2] == StatusGroup.IGNORED.tagName }?.value shouldBe 1
            metrics.find { it.labelValues[2] == StatusGroup.SERVER_ERROR.tagName }?.value shouldBe 1

        }
    }

    @Test
    @Order(7)
    fun `ignorerer path query`() = testApplication {
        application {
            installTmsApiMetrics { }
        }

        routing {
            get("/query/endpoint") {
                call.respond(HttpStatusCode.OK)
            }
        }

        client.get("query/endpoint?param1=hello")
        client.get("query/endpoint?param2=world")
        client.get("query/endpoint?param1=hello&param2=world")

        val routeLabelValues = defaultRegistry.labelValues("tms_api_call_total", "route")

        routeLabelValues shouldContain "/query/endpoint"

        routeLabelValues shouldNotContain "query/endpoint?param1=hello"
        routeLabelValues shouldNotContain "query/endpoint?param2=world"
        routeLabelValues shouldNotContain "query/endpoint?param1=hello&param2=world"


        defaultRegistry.metricFamilySamples()
            .nextElement().samples.find { it.name == "tms_api_call_total" && it.labelValues[1] == "/query/endpoint" }
            ?.value shouldBe 3
    }
}



private fun CollectorRegistry.labelValues(metric: String, label: String) = metricFamilySamples().nextElement()
    .samples.filter { it.name == metric }
    .map { it.labelValues[it.labelNames.indexOf(label)] }

private fun CollectorRegistry.assertCounterValue(counterValue: Int, clue: String = "", function: Sample.() -> Boolean) {
    metricFamilySamples().asIterator().next().samples.find { it.function() }.apply {
        withClue(clue) { (this?.value ?: 0) shouldBe counterValue }
    }
}


private fun ApplicationTestBuilder.initTestApplication(returnStatus: HttpStatusCode = HttpStatusCode.OK) = run {
    application {
        installTmsApiMetrics { setupMetricsRoute = true }
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


enum class TestStatusCode(
    val returnStatus: HttpStatusCode,
    val expectedStatusGroup: String
) {
    OK(HttpStatusCode.OK, "OK"),
    CREATED(HttpStatusCode.Created, "OK"),
    BAD_REQUEST(HttpStatusCode.BadRequest, "client_error"),
    NOT_ACCEPTABLE(HttpStatusCode.NotAcceptable, "client_error"),
    UNAUTHORIZED(HttpStatusCode.Unauthorized, "auth_issues"),
    FORBIDDEN(HttpStatusCode.Forbidden, "auth_issues"),
    SERVER_ERROR(HttpStatusCode.InternalServerError, "server_error"),
    SERVICE_UNAVAILABLE(HttpStatusCode.ServiceUnavailable, "server_error");

    val expectedStatusString = returnStatus.value.toString()
}
