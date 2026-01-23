package no.nav.tms.common.postgres

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Test query wrapper function 'single'")
class PostgresSingleTest: PostgresQueryTest() {

    @Test
    fun `can get single row`() {

        val itemOrder = ItemOrder.auto("orange", 5)

        insertOrder(itemOrder)

        val singleResult = database.single {
            queryOf("""
                    select id, item_name as itemName, amount, ordered_at as orderedAt
                    from item_order
                """
            ).map {
                ItemOrder(
                    it.string("id"),
                    it.string("itemName"),
                    it.int("amount"),
                    it.zonedDateTime("orderedAt"),
                )
            }
        }

        singleResult.let {
            it.id shouldBe itemOrder.id
            it.itemName shouldBe itemOrder.itemName
            it.amount shouldBe itemOrder.amount
            it.orderedAt shouldBe itemOrder.orderedAt
        }
    }

    @Test
    fun `returns first row from result when query returns multiple rows`() {

        val itemOrder1 = ItemOrder.auto("orange", 3)
        val itemOrder2 = ItemOrder.auto("orange", 7)
        val itemOrder3 = ItemOrder.auto("orange", 5)

        insertOrders(itemOrder1, itemOrder2, itemOrder3)

        val singleResult = database.single {
            queryOf("""
                    select id, item_name as itemName, amount, ordered_at as orderedAt
                    from item_order order by amount desc
                """
            ).map {
                ItemOrder(
                    it.string("id"),
                    it.string("itemName"),
                    it.int("amount"),
                    it.zonedDateTime("orderedAt"),
                )
            }
        }

        singleResult.let {
            it.id shouldBe itemOrder2.id
            it.itemName shouldBe itemOrder2.itemName
            it.amount shouldBe itemOrder2.amount
            it.orderedAt shouldBe itemOrder2.orderedAt
        }
    }

    @Test
    fun `throws EmptyResultException if query returned no rows`() {

        val itemOrder1 = ItemOrder.auto("orange", 3)

        insertOrder(itemOrder1)

        val appleQuery = queryOf("""
                    select id, item_name as itemName, amount, ordered_at as orderedAt
                    from item_order where item_name = 'apple'
                """
        ).map {
            ItemOrder(
                it.string("id"),
                it.string("itemName"),
                it.int("amount"),
                it.zonedDateTime("orderedAt"),
            )
        }

        shouldThrow<EmptyResultException> {
            database.single { appleQuery }
        }
    }

    @Test
    fun `can return generated values from insert statement`() {

        val (generatedId, orderedAt) = database.single {
            queryOf("""
                    insert into item_order(item_name, amount)
                    values (:itemName, :amount)
                    returning id, ordered_at as orderedAt
                """,
                mapOf(
                    "itemName" to "apple",
                    "amount" to 5,
                )
            ).map {
                it.string("id") to it.zonedDateTime("orderedAt")
            }
        }

        getOrder(generatedId).let {
            it.shouldNotBeNull()
            it.itemName shouldBe "apple"
            it.amount shouldBe 5
            it.orderedAt shouldBe orderedAt
        }
    }

    @Test
    fun `throws QueryException when encountering generic exception`() {

        val itemOrder1 = ItemOrder.auto("orange", 3)

        insertOrder(itemOrder1)

        val errorQuery = queryOf("""
                select these, fields, dont, exist
                from item_order
            """
        ).map {
            ItemOrder(
                it.string("id"),
                it.string("itemName"),
                it.int("amount"),
                it.zonedDateTime("orderedAt"),
            )
        }

        shouldThrow<QueryException> {
            database.single { errorQuery }
        }
    }
}
