package no.nav.tms.common.observability

import io.kotest.assertions.withClue
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows


class TraceloggingTest {

    @Test
    fun `Kaster feil ved ugyldig contenttype navn`() {
        assertThrowsForReason("predefinert verdi") { Domain.custom("utkast-krt") }

        assertThrowsForReason("predefinert verdi med stor bokstav") { Domain.custom("Varsel") }
        assertThrowsForReason("feilstavet predefinert verdi") { Domain.custom("mikrofrontend") }
        assertThrowsForReason("ikke tillat bokstav å") { Domain.custom("åring") }
        assertThrowsForReason("For kort") { Domain.custom("ing") }
        assertThrowsForReason("ugyldig vhar /") { Domain.custom("ing/jg") }
        assertThrowsForReason("inneholder tall") { Domain.custom("ing88") }
        assertThrowsForReason("For langt") { Domain.custom("ingnhhkdsa-sdhgjsdhgjksddjkaasd") }
        assertDoesNotThrow { Domain.custom("utbetaling") }
        assertDoesNotThrow { Domain.custom("proxy-api") }
    }
}

fun assertThrowsForReason(reason: String, block: () -> Unit) {
    withClue("$reason fører ikke til IllegalArgumentException") {
        assertThrows<IllegalArgumentException> { block() }
    }
}

