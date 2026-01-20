package no.nav.tms.common.postgres

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Test query wrapper function 'batchUpdate'")
class PostgresBatchTest: PostgresQueryTest() {

    @Test
    fun `can insert multiple rows at once`() {

        val itemOrders = listOf (
            ItemOrder.auto("apple", 3),
            ItemOrder.auto("apple", 5),
            ItemOrder.auto("apple", 7)
        )

        database.batchUpdate(
            """
                insert into item_order(id, item_name, amount, ordered_at)
                values(:id, :itemName, :amount, :orderedAt)
            """,
            itemOrders.map {
                mapOf(
                    "id" to it.id,
                    "itemName" to it.itemName,
                    "amount" to it.amount,
                    "orderedAt" to it.orderedAt
                )
            }
        )

        countOrders() shouldBe 3

        val orders = getOrders()

        orders.all { it.itemName == "apple" }
        orders.sumOf { it.amount } shouldBe 15
    }



    @Test
    fun `throws BatchUpdateException and reverts transaction if batch element raises error`() {

        val goodOrders = listOf (
            ItemOrder.auto("apple", 3),
            ItemOrder.auto("apple", 5),
            ItemOrder.auto("apple", 7)
        )

        shouldThrow<BatchUpdateException> {
            database.batchUpdate(
                """
                insert into item_order(id, item_name, amount, ordered_at)
                values(:id, :itemName, :amount, :orderedAt)
            """,
                goodOrders.map {
                    mapOf(
                        "id" to it.id,
                        "itemName" to it.itemName,
                        "amount" to it.amount,
                        "orderedAt" to it.orderedAt
                    )
                } + mapOf(
                    "id" to "badOrder",
                    "itemName" to "N/A",
                    "amount" to "NaN",
                    "orderedAt" to "infinity"
                )
            )
        }

        countOrders() shouldBe 0
    }
}
