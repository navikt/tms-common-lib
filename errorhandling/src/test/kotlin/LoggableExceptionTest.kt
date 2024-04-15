import nav.no.tms.common.testutils.assert
import org.junit.jupiter.api.Test


class LoggableExceptionTest {

    @Test
    fun secureLogInfo() {

        TestlogException().assert {

        }
    }

    class TestlogException: LoggableException(IllegalArgumentException()) {

    }
}

