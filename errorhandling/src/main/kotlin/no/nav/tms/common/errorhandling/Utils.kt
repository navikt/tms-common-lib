package no.nav.tms.common.errorhandling


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