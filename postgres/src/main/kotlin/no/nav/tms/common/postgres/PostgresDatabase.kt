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
        } catch (e: Exception) {
            if (e is SQLException && e.sqlState == PSQLState.UNIQUE_VIOLATION.state) {
                throw UniqueConstraintException(e)
            } else {
                throw QueryException("Error during 'update' query action", e)
            }
        }
    }

    fun <T> single(queryBuilder: (Session) -> NullableResultQueryAction<T>): T {
        return try {
            using(sessionOf(dataSource)) { session ->
                queryBuilder(session)
                    .let(session::run)
            } ?: throw EmptyResultException()
        } catch (e: Exception) {
            throw QueryException("Error during 'select single' query action", e)
        }
    }

    fun <T> singleOrNull(queryBuilder: (Session) -> NullableResultQueryAction<T>): T? {
        return try {
            using(sessionOf(dataSource)) { session ->
                queryBuilder(session)
                    .let(session::run)
            }
        } catch (e: Exception) {
            throw QueryException("Error during 'select single or null' query action", e)
        }
    }

    fun <T> list(queryBuilder: (Session) -> ListResultQueryAction<T>): List<T> {
        return try {
            using(sessionOf(dataSource)) { session ->
                queryBuilder(session)
                    .let(session::run)
            }
        } catch (e: Exception) {
            throw QueryException("Error during 'select list' query action", e)
        }
    }

    fun batchUpdate(statement: String, params: List<Map<String, Any?>>) {
        try {
            using(sessionOf(dataSource)) {
                it.batchPreparedNamedStatement(statement, params)
            }
        } catch (e: Exception) {
            throw BatchQueryException(e)
        }
    }
}

open class QueryException(message: String, cause: Exception?): RuntimeException(message, cause)

class EmptyResultException(): QueryException("Could not return single row because query yielded empty result.", null)
class UniqueConstraintException(cause: Exception): QueryException("Update or insert violates unique constraint", cause)
class BatchQueryException(cause: Exception): QueryException("Exception raised during batched update or insert query", cause)

