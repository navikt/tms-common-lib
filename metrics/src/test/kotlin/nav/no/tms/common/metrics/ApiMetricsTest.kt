package nav.no.tms.common.metrics

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.request.*
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
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ApiMetricsTest {

    @Test
    @Order(1)
    fun `setter opp apiMetrics`() =
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


}

private fun CollectorRegistry.assertCounterValue(counterValue: Int, function: Sample.() -> Boolean) {
    metricFamilySamples().asIterator().next().samples.find { it.function() }.apply {
        require(this != null)
        value shouldBe counterValue
    }
}


private fun ApplicationTestBuilder.initTestApplication(returnStatus: HttpStatusCode = HttpStatusCode.OK) = run {
    install(ApiResponseMetrics)
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