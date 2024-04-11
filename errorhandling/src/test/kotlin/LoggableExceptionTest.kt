import io.github.oshai.kotlinlogging.KotlinLogging
import nav.no.tms.common.testutils.assert
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class LoggableExceptionTest {

    @Test
    fun secureLogInfo() {

        TestlogException().assert {

        }
    }

    class TestlogException: LoggableException(IllegalArgumentException()) {

    }
}

