package no.nav.tms.common.logging

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import org.slf4j.LoggerFactory
import kotlin.text.indexOf

object TeamLogs {

    internal const val LOGGER_NAME = "__team_logs"
    internal const val NULL_LOGGER_NAME = "__noop_logs"
    internal const val APPENDER_NAME = "__team_logs_appender"

    private val log = KotlinLogging.logger {  }

    fun logger(failSilently: Boolean = false, caller: () -> Unit): KLogger {
        log.info { "Setter opp team-logger for ${cleanClassName(caller)}" }

        if (contextConfigured()) {
            return KotlinLogging.logger(LOGGER_NAME)
        } else if (failSilently) {
            log.warn { "Fant ikke konfigurasjon for team-logger. Ignorerer meldinger ment for team-logs." }

            return createNullLogger()
        } else {
            throw TeamLogggerNotIncludedException()
        }
    }

    private fun contextConfigured(): Boolean {
        val context = LoggerFactory.getILoggerFactory() as LoggerContext

        val teamLogsAppender = context.loggerList
            .find { it.name == LOGGER_NAME }
            ?.iteratorForAppenders()
            ?.asSequence()
            ?.find { it.name == APPENDER_NAME }

        return teamLogsAppender != null
    }

    private fun createNullLogger(): KLogger {
        val context = LoggerFactory.getILoggerFactory() as LoggerContext

        val configurator = JoranConfigurator()

        configurator.setContext(context)
        configurator.doConfigure(NULL_LOGGER_CONFIG.byteInputStream())

        return KotlinLogging.logger(NULL_LOGGER_NAME)
    }

    // Extracted from internal class KLoggerNameResolver (io.github.oshai:kotlin-logging-jvm)
    private fun cleanClassName(caller: () -> Unit): String {
        val className = caller::class.java.name

        listOf("Kt$", "$").forEach { classNameEnding ->
            val indexOfEnding = className.indexOf(classNameEnding)
            if (indexOfEnding != -1) {
                return className.take(indexOfEnding)
            }
        }
        return className
    }

    //language=xml
    private const val NULL_LOGGER_CONFIG = """
<configuration>
    <appender name="__noop_appender" class="ch.qos.logback.core.helpers.NOPAppender"/>

    <logger name="__noop_logs" additivity="false">
        <appender-ref ref="__noop_appender"/>
    </logger>
</configuration>
"""
}

class TeamLogggerNotIncludedException: IllegalStateException(
    "Fant ikke konfigurasjon for team-logger. Påse at logback.xml har linjen '<include resource=\"team-logs.xml\"/>'"
)
