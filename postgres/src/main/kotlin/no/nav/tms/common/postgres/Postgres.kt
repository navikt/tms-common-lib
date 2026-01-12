package no.nav.tms.common.postgres

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.testcontainers.postgresql.PostgreSQLContainer

object Postgres {
    fun connectWithEnv(
        jdbcUrl: String,
        hikariConfig: HikariConfig.() -> Unit = {}
    ): PostgresDatabase {

        val config = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            driverClassName = "org.postgresql.Driver"
            minimumIdle = 1
            maxLifetime = 1800000
            maximumPoolSize = 5
            connectionTimeout = 4000
            validationTimeout = 1000
            idleTimeout = 30000
            isAutoCommit = true
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        }.apply(hikariConfig)

        return PostgresDatabase(HikariDataSource(config))
    }

    fun connectWithContainer(
        container: PostgreSQLContainer,
        hikariConfig: HikariConfig.() -> Unit = {}
    ): PostgresDatabase {

        return HikariDataSource().apply {
            jdbcUrl = container.jdbcUrl
            username = container.username
            password = container.password
            isAutoCommit = true
        }
            .apply(hikariConfig)
            .apply { validate() }
            .let { PostgresDatabase(it) }
    }
}
