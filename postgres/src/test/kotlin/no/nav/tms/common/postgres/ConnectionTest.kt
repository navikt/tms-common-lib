package no.nav.tms.common.postgres

import io.kotest.assertions.throwables.shouldNotThrow
import org.junit.jupiter.api.Test

class ConnectionTest {
    @Test
    fun `can connect using jdbc url`() {
        val container = startContainer(version = "17.7")

        val authenticatedJdbcUrl = container
            .withUrlParam("user", container.username)
            .withUrlParam("password", container.password)
            .jdbcUrl

        shouldNotThrow<Exception> {
            Postgres.connectToJdbcUrl(authenticatedJdbcUrl)
        }
    }

    @Test
    fun `can connect using container`() {
        val container = startContainer(version = "17.7")

        container.start()

        shouldNotThrow<Exception> {
            Postgres.connectToContainer(container)
        }
    }
}
