package no.nav.tms.common.logging

import ch.qos.logback.classic.LoggerContext
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import org.slf4j.LoggerFactory


object TeamLogs {

    private const val LOGGER_NAME = "__team_logs"
    private const val APPENDER_NAME = "__team_logs_appender"

    fun logger(): KLogger {
        validateContext()

        return KotlinLogging.logger(LOGGER_NAME)
    }

    private fun validateContext() {
        val context = LoggerFactory.getILoggerFactory() as LoggerContext

        val teamLogsAppender = context.loggerList
            .find { it.name == LOGGER_NAME }
            ?.iteratorForAppenders()
            ?.asSequence()
            ?.find { it.name == APPENDER_NAME }

        if (teamLogsAppender == null) {
            throw TeamLogggerNotIncludedException()
        }
    }
}

class TeamLogggerNotIncludedException: IllegalStateException(
    "Fant ikke konfigurasjon for team logger. Påse at logback.xml har linjen '<include resource=\"team-logs.xml\"/>'"
)
