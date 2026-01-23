# Postgres util-bibliotek

Lite hjelpebibliotek for å koble til- og kjøre spørringer mot postgres-databaser. 

Dette biblioteket forutsetter bruk av Kotliquery og HikariCP

## Koble til database

Dette bibliteket bruker Hikari Connection Pool for å koble mot postgres. Enten via en jdbc-url, eller direkte mot en
testcontainer-instans.

Eksempel med jdbc:

```kotlin
main() {
    val jdbcUrl = System.getEnv("DB_JDBC_URL")
    
    val database = Postgres.connectToJdbcUrl(jdbcUrl)
}
```

Eksempel med docker container: 

```kotlin
object TestDB {
    private val container = PostgreSQLContainer("postgres:17.7").also {
        it.start()
    }
    val database = Postgres.connectToContainer(container)
}

```

## Hikari Connection Pool config

Default oppsett av HikariCP er som følger:

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

Dette kan videre konfigureres når en kobler til databasen. For eksempel:

```kotlin
    val jdbcUrl = System.getEnv("DB_JDBC_URL")
    
    val database = Postgres.connectToJdbcUrl(jdbcUrl) {
        isAutoCommit = false
        maximumPoolSize = 10
    }
```

Standard oppsett for tilkobling til TestContainer er:

```kotlin
HikariConfig().apply {
    isAutoCommit = true
}
```
