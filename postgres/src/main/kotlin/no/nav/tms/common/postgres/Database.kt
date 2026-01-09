package no.nav.tms.common.postgres

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.testcontainers.postgresql.PostgreSQLContainer

object Database {
    fun connectToNaisEnv(
        hikariConfig: HikariConfig.() -> Unit = {}
    ): DatabaseConnection {

        val dbUrl: String = System.getenv("DB_JDBC_URL")
            ?: throw IllegalStateException("Kan ikke opprette forbindelse mot database - mangler miljøvariabel 'DB_JDBC_URL'")

        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = dbUrl
            minimumIdle = 1
            maxLifetime = 1800000
            maximumPoolSize = 5
            connectionTimeout = 4000
            validationTimeout = 1000
            idleTimeout = 30000
            isAutoCommit = true
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        }.apply(hikariConfig)

        return DatabaseConnection(HikariDataSource(config))
    }

    fun connectToDocker(
        container: PostgreSQLContainer,
        hikariConfig: HikariConfig.() -> Unit = {}
    ): DatabaseConnection {

        val config = HikariConfig().apply {
            jdbcUrl = container.jdbcUrl
            username = container.username
            password = container.password
            isAutoCommit = true
        }.apply(hikariConfig)

        return HikariDataSource(config)
            .apply { validate() }
            .let { DatabaseConnection(it) }
    }
}
