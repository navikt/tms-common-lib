package no.nav.tms.common.postgres

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DatabindContext
import com.fasterxml.jackson.databind.DatabindException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.tms.common.postgres.JsonbHelper.json
import no.nav.tms.common.postgres.JsonbHelper.jsonOrNull
import no.nav.tms.common.postgres.JsonbHelper.toJsonb
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.postgresql.util.PGobject
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@TestInstance(PER_CLASS)
class JsonbHelperTest {
    private val container = startContainer(version = "17.7")

    private val database = Postgres.connectToContainer(container)

    private val objectMapper = jacksonObjectMapper()

    @BeforeAll
    fun createTable() {
        val query = queryOf("""
                create table event(
                    id text unique default gen_random_uuid(),
                    data jsonb,
                    time timestamp with time zone default now()
                )
            """).asExecute

        using(sessionOf(database.dataSource)) {
            it.run(query)
        }
    }

    @AfterEach
    fun cleanUp() {
        val query = queryOf("delete from event").asExecute

        using(sessionOf(database.dataSource)) {

            it.run(query)
        }
    }

    @Test
    fun `function 'toJsonb()' can serialize kotlin objects for jsonb columns`() {
        val order = Order(
            itemName = "orange",
            amount = 3
        )

        val eventId = using(sessionOf(database.dataSource)) { session ->
            val query = queryOf(
                "insert into event(data) values(:data) returning id",
                mapOf(
                    "data" to order.toJsonb()
                )
            ).map {
                it.string("id")
            }.asSingle

            session.run(query)!!
        }

        countEvents() shouldBe 1
        getEventWithSerializedData(eventId).let {
            it.shouldNotBeNull()
            objectMapper.readTree(it.eventData).let { data ->
                data["itemName"].asText() shouldBe "orange"
                data["amount"].asInt() shouldBe 3
            }
        }
    }

    @Test
    fun `can supply custom objectMapper to function 'toJsonb()'`() {
        val explicitNullsMapper = jacksonObjectMapper()
            .setDefaultPropertyInclusion(JsonInclude.Include.ALWAYS)

        val hideNullsMapper = jacksonObjectMapper()
            .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)

        val deliveryWithoutReference = Delivery(
            orderReference = null,
            address = "Business Factory Street 1"
        )

        val explicitNullsEventId = using(sessionOf(database.dataSource)) { session ->
            val query = queryOf(
                "insert into event(data) values(:data) returning id",
                mapOf(
                    "data" to deliveryWithoutReference.toJsonb(explicitNullsMapper)
                )
            ).map {
                it.string("id")
            }.asSingle

            session.run(query)!!
        }


        val hideNullsEventId = using(sessionOf(database.dataSource)) { session ->
            val query = queryOf(
                "insert into event(data) values(:data) returning id",
                mapOf(
                    "data" to deliveryWithoutReference.toJsonb(hideNullsMapper)
                )
            ).map {
                it.string("id")
            }.asSingle

            session.run(query)!!
        }

        getEventWithSerializedData(explicitNullsEventId).let {
            it.shouldNotBeNull()
            objectMapper.readTree(it.eventData).let { data ->
                data["orderReference"].shouldNotBeNull()
                data["orderReference"].isNull shouldBe true
            }
        }

        getEventWithSerializedData(hideNullsEventId).let {
            it.shouldNotBeNull()
            objectMapper.readTree(it.eventData).let { data ->
                data["orderReference"].shouldBeNull()
            }
        }
    }

    @Test
    fun `function 'jsonb()' can deserialize columns stored as jsonb`() {
        val eventId = createEvent(
            Order("apple", 5)
        )

        val orderEvent = using(sessionOf(database.dataSource)) { session ->
            val query = queryOf(
                "select id, data, time from event where id = :id",
                mapOf("id" to eventId)
            ).map {
                Event<Order>(
                    it.string("id"),
                    it.json("data"),
                    it.zonedDateTime("time"),
                )
            }.asSingle

            session.run(query)
        }

        orderEvent.shouldNotBeNull()
        orderEvent.eventData?.itemName shouldBe "apple"
        orderEvent.eventData?.amount shouldBe 5
    }

    @Test
    fun `function 'json()' fails when column is null`() {
        val eventId = createEmptyEvent()

        shouldThrow<Exception> {
            using(sessionOf(database.dataSource)) { session ->
                val query = queryOf(
                    "select id, data, time from event where id = :id",
                    mapOf("id" to eventId)
                ).map {
                    Event<Order>(
                        it.string("id"),
                        it.json("data"),
                        it.zonedDateTime("time"),
                    )
                }.asSingle

                session.run(query)
            }
        }
    }

    @Test
    fun `can supply custom objectMapper to function 'json()'`() {

        val ignoreUnknownPropertyMapper = jacksonMapperBuilder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build()

        val failOnUnknownPropertyMapper = jacksonMapperBuilder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build()

        val extendedOrderEventId = createEvent(mapOf(
            "itemName" to "orange",
            "amount" to 11,
            "category" to "fruit"
        ))

        shouldNotThrow<Exception> {
            val order = using(sessionOf(database.dataSource)) { session ->
                val query = queryOf(
                    "select data from event where id = :id",
                    mapOf("id" to extendedOrderEventId)
                ).map {
                    it.json<Order>("data", objectMapper = ignoreUnknownPropertyMapper)
                }.asSingle

                session.run(query)!!
            }

            order.itemName shouldBe "orange"
            order.amount shouldBe 11
        }

        shouldThrow<DatabindException> {
            using(sessionOf(database.dataSource)) { session ->
                val query = queryOf(
                    "select data from event where id = :id",
                    mapOf("id" to extendedOrderEventId)
                ).map {
                    it.json<Order>("data", objectMapper = failOnUnknownPropertyMapper)
                }.asSingle

                session.run(query)
            }
        }
    }

    @Test
    fun `function 'jsonOrNull()' can deserialize columns stored as jsonb`() {
        val eventId = createEvent(
            Order("banana", 7)
        )

        val orderEvent = using(sessionOf(database.dataSource)) { session ->
            val query = queryOf(
                "select id, data, time from event where id = :id",
                mapOf("id" to eventId)
            ).map {
                Event<Order>(
                    it.string("id"),
                    it.jsonOrNull("data"),
                    it.zonedDateTime("time"),
                )
            }.asSingle

            session.run(query)
        }

        orderEvent.shouldNotBeNull()
        orderEvent.eventData?.itemName shouldBe "banana"
        orderEvent.eventData?.amount shouldBe 7
    }

    @Test
    fun `function 'jsonOrNull()' can return null when column is null`() {
        val eventId = createEmptyEvent()

        val emptyEvent = using(sessionOf(database.dataSource)) { session ->
            val query = queryOf(
                "select id, data, time from event where id = :id",
                mapOf("id" to eventId)
            ).map {
                Event<Order>(
                    it.string("id"),
                    it.jsonOrNull("data"),
                    it.zonedDateTime("time"),
                )
            }.asSingle

            session.run(query)
        }

        emptyEvent.shouldNotBeNull()
        emptyEvent.eventData.shouldBeNull()
    }



    @Test
    fun `can supply custom objectMapper to function 'jsonOrNull()'`() {

        val ignoreUnknownPropertyMapper = jacksonMapperBuilder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build()

        val failOnUnknownPropertyMapper = jacksonMapperBuilder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build()

        val extendedOrderEventId = createEvent(mapOf(
            "itemName" to "orange",
            "amount" to 11,
            "category" to "fruit"
        ))

        shouldNotThrow<Exception> {
            val order = using(sessionOf(database.dataSource)) { session ->
                val query = queryOf(
                    "select data from event where id = :id",
                    mapOf("id" to extendedOrderEventId)
                ).map {
                    it.jsonOrNull<Order>("data", objectMapper = ignoreUnknownPropertyMapper)
                }.asSingle

                session.run(query)!!
            }

            order.itemName shouldBe "orange"
            order.amount shouldBe 11
        }

        shouldThrow<DatabindException> {
            using(sessionOf(database.dataSource)) { session ->
                val query = queryOf(
                    "select data from event where id = :id",
                    mapOf("id" to extendedOrderEventId)
                ).map {
                    it.jsonOrNull<Order>("data", objectMapper = failOnUnknownPropertyMapper)
                }.asSingle

                session.run(query)
            }
        }
    }

    fun countEvents(): Int {
        return using(sessionOf(database.dataSource)) { session ->
            val query = queryOf("select count(*) as count from event").map {
                it.int("count")
            }.asSingle

            session.run(query) ?: 0
        }
    }

    class Event<T>(
        val id: String,
        val eventData: T?,
        val eventTime: ZonedDateTime
    )

    class Order(
        val itemName: String,
        val amount: Int
    )

    class Delivery(
        val orderReference: String?,
        val address: String
    )

    fun getEventWithSerializedData(id: String): Event<String>? {
        return using(sessionOf(database.dataSource)) { session ->
            val query = queryOf(
                "select * from event where id = :id",
                mapOf("id" to id)
            ).map {
                Event(
                    it.string("id"),
                    it.string("data"),
                    it.zonedDateTime("time")
                )
            }.asSingle

            session.run(query)
        }
    }

    private fun <T> createEvent(event: T): String {
        return using(sessionOf(database.dataSource)) { session ->
            val query = queryOf(
                "insert into event(data) values(:data) returning id",
                mapOf(
                    "data" to objectMapper.writeValueAsString(event).let {
                        PGobject().apply {
                            type = "jsonb"
                            value = it
                        }
                    }
                )
            ).map {
                it.string("id")
            }.asSingle

            session.run(query)!!
        }
    }

    private fun createEmptyEvent(): String {
        return using(sessionOf(database.dataSource)) { session ->
            val query = queryOf(
            "insert into event(data) values(null) returning id"
            ).map {
                it.string("id")
            }.asSingle

            session.run(query)!!
        }
    }
}
