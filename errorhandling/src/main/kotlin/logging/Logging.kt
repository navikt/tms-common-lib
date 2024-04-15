package logging

import io.github.oshai.kotlinlogging.*

class TmsSecureLog private constructor(private val delegate: KLogger) : KLogger by delegate {
    init {
        require(delegate.name == "secureLog")
    }

    fun error(loggableException: LoggableException) {
        this.error(loggableException.originalThrowable) { loggableException.summary }
    }

    fun warning(loggableException: LoggableException) {
        this.warn(loggableException.originalThrowable) { loggableException.summary }
    }

    companion object {
        private const val SECURELOG_NAME = "secureLog"
        fun getSecureLog() = TmsSecureLog(KotlinLogging.logger(SECURELOG_NAME))
    }
}

class TmsLog private constructor(private val delegate: KLogger) : KLogger by delegate {
    fun error(loggableException: LoggableException) {
        this.error { loggableException.summary }
    }

    fun warning(loggableException: LoggableException) {
        this.warn { loggableException.summary }
    }

    companion object {
        fun getLog(func: () -> Unit) = TmsLog(KotlinLogging.logger(func))
    }
}


abstract class LoggableException(val originalThrowable: Throwable) : Throwable() {

    abstract val summary: String

    companion object {
        fun stackTraceSummary(throwable: Throwable) =
            throwable.stackTrace.firstOrNull()?.let { stacktraceElement ->
                """
                   Origin: ${stacktraceElement.fileName ?: "---"} ${stacktraceElement.methodName ?: "----"} linenumber:${stacktraceElement.lineNumber}
                   Message: "${throwable::class.simpleName} ${throwable.message?.let { ":$it" }}"
                """.trimIndent()
            } ?: "${throwable::class.simpleName} ${throwable.message?.let { ":$it" }}"

    }
}