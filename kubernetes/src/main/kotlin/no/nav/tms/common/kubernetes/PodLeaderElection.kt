package no.nav.tms.common.kubernetes

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.jackson.*
import java.net.InetAddress
import java.text.DateFormat
import java.time.Instant
import java.time.ZonedDateTime

class PodLeaderElection(
    private val electionPath: String = getElectionUrl(),
    private val podName: String = InetAddress.getLocalHost().hostName,
    private val queryIntervalSeconds: Long = 60L,
    private val httpClient: HttpClient = initializeClient()
) {
    private var isLeader: Boolean = false
    private var previousQuery: Instant? = null

    suspend fun isLeader(): Boolean {
        if (shouldQueryForLeader()) {
            queryForLeader()
        }

        return isLeader
    }

    private suspend fun queryForLeader() {
        val response: ElectorResponse = httpClient.get(electionPath).body()

        isLeader = response.name == podName

        previousQuery = Instant.now()
    }

    private fun shouldQueryForLeader() =
        if(previousQuery == null) {
            true
        } else {
            (Instant.now().epochSecond - previousQuery!!.epochSecond) > queryIntervalSeconds
        }


    companion object {
        private fun initializeClient() = HttpClient(Apache) {
            install(ContentNegotiation) {
                jackson {
                    registerModule(JavaTimeModule())
                    dateFormat = DateFormat.getDateTimeInstance()
                }
            }

            install(HttpTimeout)
        }

        private fun getElectionUrl(): String {

            return when (val path = System.getenv("ELECTOR_PATH")) {
                null -> throw RuntimeException("Fant ikke variabel ELECTOR_PATH")
                else -> path
            }
        }
    }
}

private data class ElectorResponse(
    val name: String,
    @JsonProperty("last_update") val lastUpdate: ZonedDateTime
)
