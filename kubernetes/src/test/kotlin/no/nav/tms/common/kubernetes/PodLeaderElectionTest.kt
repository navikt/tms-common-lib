package no.nav.tms.common.kubernetes

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import java.text.DateFormat
import java.time.ZonedDateTime

class PodLeaderElectionTest {
    private val thisPodName = "pod_name_one"
    private val otherPodName = "pod_name_two"

    @Test
    fun `identifies this instance as leader if name matches response`() = testApplication {
        val podLeaderElection = PodLeaderElection(
            podName = thisPodName,
            electionPath = "isLeader",
            httpClient = setupClient()
        )

        externalServices {
            hosts("localhost") {
                routing {
                    get("/isLeader") {
                       call.respondText(ContentType.Application.Json) {
                           """
                               {
                                   "name": "$thisPodName",
                                   "last_update": "${ZonedDateTime.now()}"
                               }
                           """.trimIndent()
                       }
                    }
                }
            }
        }

        podLeaderElection.isLeader() shouldBe true
    }

    @Test
    fun `identifies this instance as non-leader if name does not match response`() = testApplication {
        val podLeaderElection = PodLeaderElection(
            podName = thisPodName,
            electionPath = "isLeader",
            httpClient = setupClient()
        )

        externalServices {
            hosts("localhost") {
                routing {
                    get("/isLeader") {
                       call.respondText(ContentType.Application.Json) {
                           """
                               {
                                   "name": "$otherPodName",
                                   "last_update": "${ZonedDateTime.now()}"
                               }
                           """.trimIndent()
                       }
                    }
                }
            }
        }

        podLeaderElection.isLeader() shouldBe false
    }

    private fun ApplicationTestBuilder.setupClient() = client.config {
        install(ContentNegotiation) {
            jackson {
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                registerModule(JavaTimeModule())
                dateFormat = DateFormat.getDateTimeInstance()
            }
        }
    }
}
