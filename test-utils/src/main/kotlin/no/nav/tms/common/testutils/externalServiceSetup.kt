package no.nav.tms.common.testutils

import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.ktor.util.pipeline.*

/**
 * Defines a route for setup in ktor tests using externalServices
 *
 * @property path
 * @property routeMethodFunction  httpverb the route should be set up for
 * @property statusCode statuscode for the response
 * @property assert function for assertions on requests (optional)
 */
typealias HTTPVerb = Routing.(
    path: String,
    suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit
) -> Route

abstract class RouteProvider(
    val path: String,
    private val routeMethodFunction: HTTPVerb,
    private val statusCode: HttpStatusCode = OK,
    private val assert: suspend (ApplicationCall) -> Unit = {}
) {
    /**
     * Content returned in body of the response of the route
     */
    abstract fun content(): String

    /** function that adds the route in external services */
    fun Routing.initRoute() {
        routeMethodFunction(path) {
            assert(call)
            call.respondText(
                contentType = ContentType.Application.Json,
                status = statusCode,
                provider = ::content
            )
        }
    }
}

/**
 * RouteProvider with convenience method for creating graphqlresponses
 *
 * @param errorMsg message that vil be added to the error structure (optional)
 * @param path
 * @param assert function for assertions on requests (optional)
 */
abstract class GraphQlRouteProvider(
    errorMsg: String?,
    path: String,
    statusCode: HttpStatusCode = OK,
    assert: suspend (ApplicationCall) -> Unit = {},
) : RouteProvider(path, Routing::post, statusCode, assert) {
    private val errors = errorMsg?.let {
        """
                  "errors": [
                                  {
                                    "message": "$it",
                                    "locations": [
                                      {
                                        "line": 2,
                                        "column": 3
                                      }
                                    ],
                                    "path": [
                                      "journalpost"
                                    ],
                                    "extensions": {
                                      "code": "not_found",
                                      "classification": "ExecutionAborted"
                                    }
                                  }
                                ],  
                """.trimIndent()
    } ?: ""
    abstract val data: String
    override fun content(): String = """
        {
        $errors
        "data": $data
        }
    """.trimIndent()
}


fun ApplicationTestBuilder.initExternalServices(
    testHost:String,
    vararg routeProviders: RouteProvider
) = externalServices {
    hosts(testHost) {
        routing {
            routeProviders.forEach { provider ->
                provider.run {
                    this@routing.initRoute()
                }
            }
        }
    }
}

