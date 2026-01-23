package no.nav.tms.common.postgres

import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.postgresql.PostgreSQLContainer

// Workaround for colima docker. Default wait strategy for PostgreSQLContainer is to wait for
// log pattern ".*database system is ready to accept connections.*\\s". However, colima is slightly too slow to
// open the port to the postgres container, which leads to an immediate initialization error. It seems sufficient
// to change container wait strategy to wait for listening port
fun startContainer(version: String): PostgreSQLContainer {
    return PostgreSQLContainer("postgres:$version").also { container ->
        container.setWaitStrategy(Wait.forListeningPort())
        container.start()
    }
}
