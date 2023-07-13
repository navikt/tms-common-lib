package nav.no.tms.common.metrics

import io.kotest.matchers.shouldBe
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test

class ApiMetricsTest {

    @Test
    fun `skal installere apimetrics`() =
        testApplication {
            install(ApiResponseMetrics)
            false shouldBe true
        }
}
