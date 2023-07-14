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
import org.junit.jupiter.api.Test
import java.util.*


class ApiMetricsTest {

    @Test
    fun `setter opp apiMetrics`() =
        testApplication {
            install(ApiResponseMetrics)

            routing {
                get("test") {
                    call.respond(HttpStatusCode.OK)
                }
            }

            client.withAuthHeader("test", "unknown")

            val collected = defaultRegistry.metricFamilySamples().asIterator().next()

            collected.samples.first().apply {
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
    fun `henter ut sensitivitet`() =
        testApplication {
            install(ApiResponseMetrics)
            install(Authentication) {
                jwt {
                    skipWhen { true }
                }
            }

            routing {
                authenticate {
                    get("test/{status?}") {
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }

            client.withAuthHeader("/test", "level4")
            defaultRegistry.assertCounterValue(1) { labelValues[3] == "high" }

            client.withAuthHeader("/test", "idporten-loa-high")
            defaultRegistry.assertCounterValue(2) { labelValues[3] == "high" }

            client.withAuthHeader("/test", "idporten-loa-substantial")
            defaultRegistry.assertCounterValue(1) { labelValues[3] == "substantial" }

            client.withAuthHeader("/test", "level3")
            defaultRegistry.assertCounterValue(2) { labelValues[3] == "substantial" }

            client.get("/test")
            defaultRegistry.assertCounterValue(1) { labelValues[3] == "NA" }

        }
}

private fun CollectorRegistry.assertCounterValue(counterValue: Int, function: Sample.() -> Boolean) {
    metricFamilySamples().asIterator().next().samples.find { it.function() }.apply {
        require(this != null)
        value shouldBe counterValue
    }
}


private suspend fun HttpClient.withAuthHeader(url: String, acr: String, authHeaderName: String = "Authorization") {
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
