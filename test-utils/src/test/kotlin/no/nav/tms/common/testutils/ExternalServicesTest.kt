package no.nav.tms.common.testutils

import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opentest4j.AssertionFailedError

class ExternalServicesTest {
    private val testHost = "http://testing.tests"

    @Test
    fun `Setter opp ruter`() = testApplication {
        initExternalServices(
            testHost,
            GetTestRouteProvider(),
            PostTestRouteProvider(),
            PostTestRouteProvider("other/path")
        )

        client.get("$testHost/a/path").assert {
            this.status shouldBe HttpStatusCode.OK
            this.bodyAsText() shouldBe "Superfun!a/path"
        }
        client.post("$testHost/a/path").assert {
            this.status shouldBe HttpStatusCode.OK
            this.bodyAsText() shouldBe "Superfun post!a/path"
        }
        client.post("$testHost/other/path").assert {
            this.status shouldBe HttpStatusCode.OK
            this.bodyAsText() shouldBe "Superfun post!other/path"
        }
    }

    @Test
    fun `setter opp ruter med forskjellige statuskoder`() = testApplication {
        initExternalServices(
            testHost,
            StatusTestRouteProvider("servererror", HttpStatusCode.InternalServerError),
            StatusTestRouteProvider("multi", HttpStatusCode.MultiStatus),
            StatusTestRouteProvider("not/allowed", HttpStatusCode.MethodNotAllowed),
        )

        client.get("$testHost/servererror").status shouldBe HttpStatusCode.InternalServerError
        client.get("$testHost/multi").status shouldBe HttpStatusCode.MultiStatus
        client.get("$testHost/not/allowed").status shouldBe HttpStatusCode.MethodNotAllowed
    }

    @Test
    fun `utfører assertions`() = testApplication {

        initExternalServices(
            testHost,
            AssertionsTestRouteProvider("Stringy string")
        )

        assertThrows<AssertionFailedError> { client.get("$testHost/assert") }
        assertThrows<AssertionFailedError> {
            client.get("$testHost/assert") {
                setBody("Not as stringy as you'd think")
            }
        }
        client.get("$testHost/assert") {
            setBody("Stringy string")
        }.status shouldBe HttpStatusCode.OK
    }


    class GetTestRouteProvider(
        path: String = "a/path",
    ) : RouteProvider(path = path, routeMethodFunction = Routing::get) {
        override fun content(): String = "Superfun!$path"
    }

    class StatusTestRouteProvider(
        path: String,
        statusCode: HttpStatusCode,
    ) : RouteProvider(path = path, statusCode = statusCode, routeMethodFunction = Routing::get) {
        override fun content(): String = ""
    }

    class AssertionsTestRouteProvider(
        expectedString: String,
    ) : RouteProvider(path = "assert", routeMethodFunction = Routing::get,
        assert = { call -> call.receiveText() shouldBe expectedString }) {
        override fun content(): String = "Hurra!"
    }

    class PostTestRouteProvider(
        path: String = "a/path",
    ) : RouteProvider(path = path, routeMethodFunction = Routing::post) {
        override fun content(): String = "Superfun post!$path"
    }
}
