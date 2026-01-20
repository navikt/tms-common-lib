# Postgres convenience-library

Small convenience library for connecting to- and querying a postgres database

## Connecting to database

This library uses Hikari Connection Pool to connect to postgres. The API enables connecting to a JDBC-URL or directly 
to a Docker-instance. 

Example using jdbc:

```kotlin
main() {
    val jdbcUrl = System.getEnv("DB_JDBC_URL")
    
    val database = Postgres.connectToJdbcUrl(jdbcUrl)
}
```

Example using docker container: 

```kotlin
object TestDB {
    private val container = PostgreSQLContainer("postgres:17.7").also {
        it.start()
    }
    val database = Postgres.connectToContainer(container)
}

```

## Hikari Connection Pool config

The default config for HikariCP when connecting using jdbc is:

```kotlin
HikariConfig().apply {
    driverClassName = "org.postgresql.Driver"
    minimumIdle = 1
    maxLifetime = 1800000
    maximumPoolSize = 5
    connectionTimeout = 4000
    validationTimeout = 1000
    idleTimeout = 30000
    isAutoCommit = true
    transactionIsolation = "TRANSACTION_REPEATABLE_READ"
}
```

This can be further changed when connecting to the database. For example:

```kotlin
    val jdbcUrl = System.getEnv("DB_JDBC_URL")
    
    val database = Postgres.connectToJdbcUrl(jdbcUrl) {
        isAutoCommit = false
        maximumPoolSize = 10
    }
```

The default config when connecting to docker container is:

```kotlin
HikariConfig().apply {
    isAutoCommit = true
}
```
