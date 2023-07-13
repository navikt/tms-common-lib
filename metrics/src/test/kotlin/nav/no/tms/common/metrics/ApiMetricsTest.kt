package nav.no.tms.common.metrics

import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.prometheus.client.CollectorRegistry
import org.junit.jupiter.api.Test

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

            client.get("test")

            val collected = CollectorRegistry.defaultRegistry.metricFamilySamples().asIterator().next()

            collected.samples.first().apply {
                this.labelNames[0] shouldBe "status"
                this.labelValues[0] shouldBe "200"
                this.labelNames[1] shouldBe "route"
                this.labelValues[1] shouldBe "/test"
                this.labelNames[2] shouldBe "statusgroup"
                this.labelValues[2] shouldBe "OK"
                this.labelNames[3] shouldBe "acr"
                this.labelValues[3] shouldBe "NA"
            }
        }
}
