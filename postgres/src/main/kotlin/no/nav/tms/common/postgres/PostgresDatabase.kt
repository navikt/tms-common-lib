package no.nav.tms.common.postgres

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Query
import kotliquery.Session
import kotliquery.action.ListResultQueryAction
import kotliquery.action.NullableResultQueryAction

import kotliquery.sessionOf
import kotliquery.using
import org.postgresql.util.PSQLState
import java.sql.SQLException

class PostgresDatabase internal constructor(
    val dataSource: HikariDataSource
) {
    fun update(queryBuilder: (Session) -> Query): Int {
        return try {
            using(sessionOf(dataSource)) { session ->
                queryBuilder(session)
                    .asUpdate
                    .let(session::run)
            }
        } catch (e: SQLException) {
            if (e.sqlState == PSQLState.UNIQUE_VIOLATION.state) {
                throw UniqueConstraintException()
            } else {
                throw e
            }
        }
    }

    fun <T> single(queryBuilder: (Session) -> NullableResultQueryAction<T>): T {
        return using(sessionOf(dataSource)) { session ->
            queryBuilder(session)
                .let(session::run)
        } ?: throw EmptyResultException()
    }

    fun <T> singleOrNull(queryBuilder: (Session) -> NullableResultQueryAction<T>): T? {
        return using(sessionOf(dataSource)) { session ->
            queryBuilder(session)
                .let(session::run)
        }
    }

    fun <T> list(queryBuilder: (Session) -> ListResultQueryAction<T>): List<T> {
        return using(sessionOf(dataSource)) { session ->
            queryBuilder(session)
                .let(session::run)
        }
    }

    fun batch(statement: String, params: List<Map<String, Any?>>) {
        using(sessionOf(dataSource)) {
            it.batchPreparedNamedStatement(statement, params)
        }
    }
}

class EmptyResultException(): RuntimeException()
class UniqueConstraintException(): RuntimeException()

