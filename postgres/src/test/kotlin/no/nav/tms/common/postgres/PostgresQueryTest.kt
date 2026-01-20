package no.nav.tms.common.postgres

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@TestInstance(PER_CLASS)
open class PostgresQueryTest {
    protected val container = startContainer(version = "17.7")

    protected val database = Postgres.connectToContainer(container)

    @BeforeAll
    fun createTable() {
        val query = queryOf("""
                create table item_order(
                    id text unique default gen_random_uuid(),
                    item_name text not null,
                    amount int not null,
                    ordered_at timestamp with time zone default now()
                )
            """).asExecute

        using(sessionOf(database.dataSource)) {
            it.run(query)
        }
    }

    @AfterEach
    fun cleanUp() {
        val query = queryOf("delete from item_order").asExecute

        using(sessionOf(database.dataSource)) {

            it.run(query)
        }
    }

    protected class ItemOrder(
        val id: String,
        val itemName: String,
        val amount: Int,
        val orderedAt: ZonedDateTime = ZonedDateTime.now().truncatedTo(ChronoUnit.MILLIS)
    ) {
        companion object {
            fun auto(
                itemName: String,
                amount: Int
            ) = ItemOrder(
                id = UUID.randomUUID().toString(),
                itemName = itemName,
                amount = amount,
                orderedAt = ZonedDateTime.now().truncatedTo(ChronoUnit.MILLIS)
            )
        }
    }

    protected fun countOrders(): Int {
        return using(sessionOf(database.dataSource)) { session ->
            val query = queryOf("select count(*) as count from item_order").map {
                it.int("count")
            }.asSingle

            session.run(query) ?: 0
        }
    }

    protected fun getOrders(): List<ItemOrder> {
        return using(sessionOf(database.dataSource)) { session ->
            val query = queryOf("select * from item_order").map {
                ItemOrder(
                    it.string("id"),
                    it.string("item_name"),
                    it.int("amount"),
                    it.zonedDateTime("ordered_at")
                )
            }.asList

            session.run(query)
        }
    }

    protected fun getOrder(id: String): ItemOrder? {
        return using(sessionOf(database.dataSource)) { session ->
            val query = queryOf(
                "select * from item_order where id = :id",
                mapOf("id" to id)
            ).map {
                ItemOrder(
                    it.string("id"),
                    it.string("item_name"),
                    it.int("amount"),
                    it.zonedDateTime("ordered_at")
                )
            }.asSingle

            session.run(query)
        }
    }

    protected fun insertOrder(itemOrder: ItemOrder) {
        using(sessionOf(database.dataSource)) { session ->
            val query = queryOf("""
                    insert into item_order(id, item_name, amount, ordered_at)
                    values(:id, :itemName, :amount, :orderedAt)
                """,
                mapOf(
                    "id" to itemOrder.id,
                    "itemName" to itemOrder.itemName,
                    "amount" to itemOrder.amount,
                    "orderedAt" to itemOrder.orderedAt
                )
            ).asUpdate

            session.run(query)
        }
    }

    protected fun insertOrders(vararg orders: ItemOrder) {
        orders.forEach(::insertOrder)
    }
}
