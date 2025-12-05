package no.nav.tms.common.logging

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory


object TeamLogsConfigTest {

    @Test
    fun `kaster ikke feil dersom logback-fil inkluderer team-logs-fil`() {
        loadLogbackFile("med-include.xml")

        shouldNotThrow<TeamLogggerNotIncludedException> {
            TeamLogs.logger { }
        }
    }

    @Test
    fun `kaster feil dersom logback-fil ikke inkluderer team-logs-fil`() {
        loadLogbackFile("uten-include.xml")


        shouldThrow<TeamLogggerNotIncludedException> {
            TeamLogs.logger { }
        }
    }

    @Test
    fun `kaster ikke feil dersom logback-fil inkluderer team-logs-for-test-fil`() {
        loadLogbackFile("med-include-test.xml")

        shouldNotThrow<TeamLogggerNotIncludedException> {
            TeamLogs.logger { }
        }
    }

    @Test
    fun `logger advarser og returnerer null-logger hvis failSilently er satt og team-logs ikke er inkludert`() {
        loadLogbackFile("uten-include.xml")

        shouldNotThrow<TeamLogggerNotIncludedException> {
            TeamLogs.logger(failSilently = true) {}
        }
    }

    private fun loadLogbackFile(fileName: String) {

        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        val config = JoranConfigurator()

        loggerContext.reset()
        config.setContext(loggerContext)
        config.doConfigure("src/test/resources/$fileName")
    }
}
