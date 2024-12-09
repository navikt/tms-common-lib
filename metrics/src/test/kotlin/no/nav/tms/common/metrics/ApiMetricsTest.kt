package no.nav.tms.common.metrics

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
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
import io.prometheus.metrics.model.registry.MetricNameFilter
import io.prometheus.metrics.model.registry.PrometheusRegistry
import io.prometheus.metrics.model.registry.PrometheusRegistry.defaultRegistry
import io.prometheus.metrics.model.snapshots.CounterSnapshot.CounterDataPointSnapshot
import io.prometheus.metrics.model.snapshots.DataPointSnapshot
import io.prometheus.metrics.model.snapshots.Labels
import no.nav.tms.common.metrics.StatusGroup.Companion.belongsTo
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
                defaultRegistry.scrape().first().dataPoints.first().labels
            require(collected != null)
            collected.apply {
                getName(0) shouldBe "acr"
                getValue(0) shouldBe "unknown"
                getName(1) shouldBe "route"
                getValue(1) shouldBe "/test"
                getName(2) shouldBe "status"
                getValue(2) shouldBe "200"
                getName(3) shouldBe "statusgroup"
                getValue(3) shouldBe "OK"
            }
        }

    @Test
    @Order(2)
    fun `henter ut sensitivitet`() =
        testApplication {
            initTestApplication()

            client.getwithAuthHeader(acr = "level4")
            defaultRegistry.assertCounterValue(1) { getName(3) == "high" }

            client.getwithAuthHeader(acr = "idporten-loa-high")
            defaultRegistry.assertCounterValue(2) { getName(3) == "high" }

            client.getwithAuthHeader(acr = "idporten-loa-substantial")
            defaultRegistry.assertCounterValue(1) { getName(3) == "substantial" }

            client.getwithAuthHeader(acr = "level3")
            defaultRegistry.assertCounterValue(2) { getName(3) == "substantial" }

            client.get("/test")
            defaultRegistry.assertCounterValue(1) { getName(3) == "NA" }

        }

    @ParameterizedTest
    @Order(3)
    @EnumSource(TestStatusCode::class)
    fun `mapper status riktig`(code: TestStatusCode) = testApplication {
        initTestApplication(code.returnStatus)
        client.getwithAuthHeader(acr = "something")

        val sample = defaultRegistry.scrape().first().dataPoints.find {
            it.labels.getValue(2) == code.expectedStatusString
        }

        require(sample != null)
        sample.labels.getValue(3) shouldBe code.expectedStatusGroup
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

            defaultRegistry.assertCounterValue(0) { getValue(1) == "/ignore" }
            defaultRegistry.assertCounterValue(1) { getValue(1) == "/dontignore" }
            defaultRegistry.assertCounterValue(1) { getValue(1) == "/dontignoreforthisstatus" }
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

            val metrics = defaultRegistry.scrape().first { it.metadata.name == "tms_api_call" }
                .dataPoints.map { it as CounterDataPointSnapshot }
                .filter { it.labels.getValue(1) == "/map" }

            metrics.find { it.labels.getValue(3) == StatusGroup.OK.tagName }?.value shouldBe 2
            metrics.find { it.labels.getValue(3) == StatusGroup.IGNORED.tagName }?.value shouldBe 1
            metrics.find { it.labels.getValue(3) == StatusGroup.SERVER_ERROR.tagName }?.value shouldBe 1

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

        client.get("query/endpoint?param1=hello").status shouldBe HttpStatusCode.OK
        client.get("query/endpoint?param2=world")
        client.get("query/endpoint?param1=hello&param2=world")

        val routeLabelValues = defaultRegistry.labelValues("tms_api_call", "route")

        routeLabelValues shouldContain "/query/endpoint"

        routeLabelValues shouldNotContain "query/endpoint?param1=hello"
        routeLabelValues shouldNotContain "query/endpoint?param2=world"
        routeLabelValues shouldNotContain "query/endpoint?param1=hello&param2=world"


        defaultRegistry.scrape().first { it.metadata.name == "tms_api_call" }
            .dataPoints.find { it.labels.getValue(1) == "/query/endpoint" }
            ?.let { it as CounterDataPointSnapshot }
            ?.value shouldBe 3
    }

    @Test
    @Order(8)
    fun `maskerer path variabler`() = testApplication {
        application {
            installTmsApiMetrics {

            }
        }

        routing {
            get("/query/endpoint") {
                call.respond(HttpStatusCode.OK)
            }
            get("/get/resource/{name}/with/id/{id}") {
                call.respond(HttpStatusCode.OK)
            }
        }

        client.get("/get/resource/cake/with/id/123?mything=hello")
        client.get("/get/resource/cookie/with/id/456")
        client.get("/get/resource/fruit/with/id/789")

        defaultRegistry.scrape().first { it.metadata.name == "tms_api_call" }
            .dataPoints.find { it.labels.getValue(1) == "/get/resource/{name}/with/id/{id}" }
            ?.let { it as CounterDataPointSnapshot }
            ?.value shouldBe 3

        val routeLabelValues = defaultRegistry.labelValues("tms_api_call", "route")

        routeLabelValues shouldContain "/get/resource/{name}/with/id/{id}"

        routeLabelValues shouldNotContain "/get/resource/cake/with/id/123"
        routeLabelValues shouldNotContain "/get/resource/cookie/with/id/456"
        routeLabelValues shouldNotContain "/get/resource/fruit/with/id/789"
    }
}


private fun PrometheusRegistry.labelValues(metric: String, label: String) = scrape()
    .first { it.metadata.name == metric }
    .dataPoints
    .map { it.labels.get(label) }

private fun PrometheusRegistry.assertCounterValue(counterValue: Int, clue: String = "", function: Labels.() -> Boolean) {
    scrape().iterator().next()
        .dataPoints.find { it.labels.function() }
        ?.takeIf { it is CounterDataPointSnapshot }
        ?.apply {
            withClue(clue) { ((this as CounterDataPointSnapshot).value) shouldBe counterValue
        }
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
