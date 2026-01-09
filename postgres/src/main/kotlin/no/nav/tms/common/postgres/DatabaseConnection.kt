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

class DatabaseConnection internal constructor(
    private val dataSource: HikariDataSource
) {
    fun update(queryBuilder: (Session) -> Query): Int {
        return try {
            using(sessionOf(dataSource)) {
                it.run(queryBuilder.invoke(it).asUpdate)
            }
        } catch (e: SQLException) {
            if (e.sqlState == PSQLState.UNIQUE_VIOLATION.state) {
                throw UniqueConstraintException()
            } else {
                throw e
            }
        }
    }

    fun <T> single(action: () -> NullableResultQueryAction<T>): T {
        return using(sessionOf(dataSource)) {
            it.run(action.invoke())
        } ?: throw EmptyResultException()
    }

    fun <T> singleOrNull(action: () -> NullableResultQueryAction<T>): T? {
        return using(sessionOf(dataSource)) {
            it.run(action.invoke())
        }
    }

    fun <T> list(action: () -> ListResultQueryAction<T>): List<T> {
        return using(sessionOf(dataSource)) {
            it.run(action.invoke())
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

