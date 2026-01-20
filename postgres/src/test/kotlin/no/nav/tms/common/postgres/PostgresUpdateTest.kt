package no.nav.tms.common.postgres

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@DisplayName("Test query wrapper function 'update'")
class PostgresUpdateTest: PostgresQueryTest() {

    @Test
    fun `can insert row`() {
        val orderedAt = ZonedDateTime.now().truncatedTo(ChronoUnit.MILLIS)

        database.update {
            queryOf("""
                    insert into item_order(id, item_name, amount, ordered_at)
                    values(:id, :itemName, :amount, :orderedAt)
                """,
                mapOf(
                    "id" to "123",
                    "itemName" to "apple",
                    "amount" to 5,
                    "orderedAt" to orderedAt
                )
            )
        }

        countOrders() shouldBe 1
        getOrder("123").let {
            it.shouldNotBeNull()
            it.itemName shouldBe "apple"
            it.amount shouldBe 5
            it.orderedAt shouldBe orderedAt
        }
    }

    @Test
    fun `can update row`() {

        val itemOrder = ItemOrder(
            id = "456",
            itemName = "orange",
            amount = 7
        )

        insertOrder(itemOrder)

        database.update {
            queryOf(
                """
                    update item_order set amount = 13 where id = '456'
                """
            )
        }

        countOrders() shouldBe 1
        getOrder("456").let {
            it.shouldNotBeNull()
            it.itemName shouldBe "orange"
            it.amount shouldBe 13
            it.orderedAt shouldBe itemOrder.orderedAt
        }
    }

    @Test
    fun `can delete row`() {

        val itemOrder = ItemOrder(
            id = "456",
            itemName = "orange",
            amount = 7
        )

        insertOrder(itemOrder)

        database.update {
            queryOf(
                """
                    delete from item_order where id = '456'
                """
            )
        }

        countOrders() shouldBe 0
        getOrder("456").let {
            it.shouldBeNull()
        }
    }

    @Test
    fun `returns number of affected or created rows`() {

        val appleOrder1 = ItemOrder("1", "apple", 3)
        val appleOrder2 = ItemOrder("2", "apple", 5)
        val appleOrder3 = ItemOrder("3", "apple", 7)
        val orangeOrder2 = ItemOrder("4", "orange", 2)
        val orangeOrder3 = ItemOrder("5", "orange", 3)

        insertOrders(appleOrder1, appleOrder2, appleOrder3, orangeOrder2, orangeOrder3)

        val updates = database.update {
            queryOf(
                """
                    update item_order set amount = 2 where item_name = 'orange'
                """
            )
        }

        val deletes = database.update {
            queryOf(
                """
                    delete from item_order where item_name = 'apple'
                """
            )
        }

        countOrders() shouldBe 2
        updates shouldBe 2
        deletes shouldBe 3
    }

    @Test
    fun `throws UniqueConstraintException when violating unique constraint with insert`() {

        val existingOrder = ItemOrder("1", "apple", 3)

        insertOrder(existingOrder)

        val insertQuery = queryOf("""
                    insert into item_order(id, item_name, amount, ordered_at)
                    values(:id, :itemName, :amount, :orderedAt)
                """,
            mapOf(
                "id" to existingOrder.id,
                "itemName" to "orange",
                "amount" to 5,
                "orderedAt" to ZonedDateTime.now()
            )
        )

        shouldThrow<UniqueConstraintException> {
            database.update { insertQuery }
        }
    }

    @Test
    fun `throws UniqueConstraintException when violating unique constraint with update`() {

        val existingOrder1 = ItemOrder("1", "apple", 3)
        val existingOrder2 = ItemOrder("2", "apple", 5)

        insertOrders(existingOrder1, existingOrder2)

        val updateQuery = queryOf("""
                    update item_order set id = :id1 where id = :id2
                """,
            mapOf(
                "id1" to existingOrder1.id,
                "id2" to existingOrder2.id,
            )
        )

        shouldThrow<UniqueConstraintException> {
            database.update { updateQuery }
        }
    }


    @Test
    fun `throws QueryException when encountering generic error`() {

        val insertQuery = queryOf("""
                    insert into item_order(id, item_name, amount, ordered_at)
                    values(:id, :itemName, :amount, :orderedAt)
                """,
            mapOf(
                "id" to "123",
                "itemName" to "orange",
                "amount" to "not a number",
                "orderedAt" to ZonedDateTime.now()
            )
        )

        shouldThrow<QueryException> {
            database.update { insertQuery }
        }
    }
}
