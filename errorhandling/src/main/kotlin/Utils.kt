import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging




abstract class LoggableException(private val originalThrowable: Throwable) : Throwable() {
    val shortStackTrace: String = stackTraceSummary(originalThrowable)
    val errorSummary: String = ""

    val detailedErrorDescription =
        """ $errorSummary
            $shortStackTrace
    """.trimIndent()
    fun KLogger.secureLogInfo() {
        require(this.name == "secureLog")
        this@secureLogInfo.info(originalThrowable.cause) { this@LoggableException.errorSummary }
    }

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

fun main() {
    val log = KotlinLogging.logger {  }
    object: LoggableException(IllegalArgumentException()){
    }.apply { log.secureLogInfo() }


}

fun String.redactedMessage(keepAll: Boolean = false): String =
    replace(Regex("\\d{11}"), "**READCTED**")
        .let {
            if (keepAll)
                it
            else
                substringOrAll(0..50)
        }

private fun String.substringOrAll(intRange: IntRange): String =
    if (intRange.last > this.length) this
    else substring(0, intRange.last)