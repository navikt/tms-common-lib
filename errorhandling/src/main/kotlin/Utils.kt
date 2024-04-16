package nav.no.tms.common.errohandling


fun String.redactedMessage(keepAll: Boolean = false): String =
    replace(Regex("\\d{11}"), "**REDACTED**")
        .let {
            if (keepAll)
                it
            else
                substringOrAll(0..100)
        }

private fun String.substringOrAll(intRange: IntRange): String =
    if (intRange.last > this.length) this
    else substring(0, intRange.last)