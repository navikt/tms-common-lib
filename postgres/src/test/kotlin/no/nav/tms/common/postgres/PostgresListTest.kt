package no.nav.tms.common.postgres

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Test query wrapper function 'list'")
class PostgresListTest: PostgresQueryTest() {

    @Test
    fun `can get multiple rows`() {

        val itemOrder1 = ItemOrder.auto("apple", 3)
        val itemOrder2 = ItemOrder.auto("orange", 5)
        val itemOrder3 = ItemOrder.auto("apple", 7)

        insertOrders(itemOrder1, itemOrder2, itemOrder3)

        val listResult = database.list {
            queryOf("""
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
        }

        listResult.size shouldBe 2
        listResult[0].let {
            it.id shouldBe itemOrder1.id
            it.itemName shouldBe itemOrder1.itemName
            it.amount shouldBe itemOrder1.amount
            it.orderedAt shouldBe itemOrder1.orderedAt
        }
        listResult[1].let {
            it.id shouldBe itemOrder3.id
            it.itemName shouldBe itemOrder3.itemName
            it.amount shouldBe itemOrder3.amount
            it.orderedAt shouldBe itemOrder3.orderedAt
        }
    }

    @Test
    fun `returns empty list if query returns no rows`() {

        val appleOrder = ItemOrder.auto("orange", 3)
        val orangeOrder = ItemOrder.auto("apple", 5)

        insertOrders(appleOrder, orangeOrder)

        val bananaOrders = database.list {
            queryOf("""
                    select id, item_name as itemName, amount, ordered_at as orderedAt
                    from item_order where item_name = 'banana'
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

        bananaOrders.shouldBeEmpty()
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
            database.list { errorQuery }
        }
    }
}
