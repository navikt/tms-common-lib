package no.nav.tms.common.util.scheduling

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Duration

class PeriodicJobTest {

    @Test
    fun `runs job every given interval`() {
        var ticks = 0

        val ticker = object : PeriodicJob(interval = Duration.ofMillis(100)) {
            override val job = initializeJob {
                ticks += 1
            }
        }

        runBlocking {
            ticker.start()

            delay(350)

            ticker.stop()
        }

        ticks shouldBe 4 // Ticks once immediately then once every 100 ms
    }
}
