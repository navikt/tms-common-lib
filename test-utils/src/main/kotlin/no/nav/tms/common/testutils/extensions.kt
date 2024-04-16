package no.nav.tms.common.testutils

inline fun <T> T.assert(block: T.() -> Unit): T =
    apply {
        block()
    }
