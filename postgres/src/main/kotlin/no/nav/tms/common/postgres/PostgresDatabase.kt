package no.nav.tms.common.postgres

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Query
import kotliquery.Session
import kotliquery.action.ResultQueryActionBuilder

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

    fun <T> single(queryBuilder: (Session) -> ResultQueryActionBuilder<T>): T {
        return try {
            using(sessionOf(dataSource)) { session ->
                queryBuilder(session)
                    .asSingle
                    .let(session::run)
            }
        } catch (e: Exception) {
            throw QueryException("Error during 'single' query action", e)
        } ?: throw EmptyResultException()
    }

    fun <T> singleOrNull(queryBuilder: (Session) -> ResultQueryActionBuilder<T>): T? {
        return try {
            using(sessionOf(dataSource)) { session ->
                queryBuilder(session)
                    .asSingle
                    .let(session::run)
            }
        } catch (e: Exception) {
            throw QueryException("Error during 'single or null' query action", e)
        }
    }

    fun <T> list(queryBuilder: (Session) -> ResultQueryActionBuilder<T>): List<T> {
        return try {
            using(sessionOf(dataSource)) { session ->
                queryBuilder(session)
                    .asList
                    .let(session::run)
            }
        } catch (e: Exception) {
            throw QueryException("Error during 'list' query action", e)
        }
    }

    fun batchUpdate(statement: String, params: List<Map<String, Any?>>) {
        try {
            using(sessionOf(dataSource)) {
                it.batchPreparedNamedStatement(statement, params)
            }
        } catch (e: Exception) {
            throw BatchUpdateException(e)
        }
    }
}

open class QueryException(message: String, cause: Exception?): RuntimeException(message, cause)

class EmptyResultException(): QueryException("Could not return single row because query yielded empty result.", null)
class UniqueConstraintException(cause: Exception): QueryException("Update or insert violates unique constraint", cause)
class BatchUpdateException(cause: Exception): QueryException("Exception raised during batched update or insert query", cause)

