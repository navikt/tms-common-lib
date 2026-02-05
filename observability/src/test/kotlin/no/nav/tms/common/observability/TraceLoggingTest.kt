package no.nav.tms.common.observability

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import no.nav.tms.common.observability.Contenttype.Companion.utkast
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.MDC


class TraceloggingTest {
    @Test
    fun `Legger til isolert min side kontekst felter`() {
        withMinSideLoggContext(
            minSideId = "En id",
            contenttype = utkast,
            producedBy = "ett team"

        ) {
            val mdc = MDC.getCopyOfContextMap()
            mdc.size shouldBe 3
            mdc["minside_id"] shouldBe "En id"
            mdc["contenttype"] shouldBe "utkast"
            mdc["produced_by"] shouldBe "ett team"
        }
        MDC.getCopyOfContextMap().size shouldBe 0
    }

    @Test
    fun `Legger til context fra definert objekt`() {
        val testObject = object : MinSideContext {
            override val minSideId: String = "MinSideId"
            override val contenttype: Contenttype = Contenttype.varsel
            override val producedBy: String = "Et team"
            override val extraFields: Map<String, String> = mapOf("felt1" to "verdi1", "felt2" to "verdi2")
        }

        withMinSideLoggContext(testObject) {
            val mdc = MDC.getCopyOfContextMap()
            mdc.size shouldBe 5
            mdc["minside_id"] shouldBe "MinSideId"
            mdc["contenttype"] shouldBe "varsel"
            mdc["produced_by"] shouldBe "Et team"
            mdc["felt1"] shouldBe "verdi1"
            mdc["felt2"] shouldBe "verdi2"
        }
        MDC.getCopyOfContextMap().size shouldBe 0
    }

    @Test
    fun `Kaster feil ved ugyldig contenttype navn`() {
        assertThrowsForReason("predefinert verdi") { Contenttype.custom("utkast-krt") }

        assertThrowsForReason("predefinert verdi med stor bokstav") { Contenttype.custom("Varsel") }
        assertThrowsForReason("feilstavet predefinert verdi") { Contenttype.custom("mikrofrontend") }
        assertThrowsForReason("ikke tillat bokstav å") { Contenttype.custom("åring") }
        assertThrowsForReason("For kort") { Contenttype.custom("ing") }
        assertThrowsForReason("ugyldig vhar /") { Contenttype.custom("ing/jg") }
        assertThrowsForReason("inneholder tall") { Contenttype.custom("ing88") }
        assertThrowsForReason("For langt") { Contenttype.custom("ingnhhkdsa-sdhgjsdhgjksddjkaasd") }
        assertDoesNotThrow { Contenttype.custom("utbetaling") }
        assertDoesNotThrow { Contenttype.custom("proxy-api") }
    }
}

fun assertThrowsForReason(reason: String, block: () -> Unit) {
    withClue("$reason fører ikke til IllegalArgumentException") {
        assertThrows<IllegalArgumentException> { block() }
    }
}

