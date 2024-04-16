package nav.no.tms.common.errohandling.http

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.net.SocketTimeoutException


suspend fun HttpClient.getFromService(url: String, service: String, block: HttpRequestBuilder.() -> Unit = {}) =
    requestCatching(url, service) {
        get(url) {
            expectSuccess = true
            block()
        }
    }

suspend fun HttpClient.postToService(url: String, service: String, block: HttpRequestBuilder.() -> Unit = {}) =
    requestCatching(url, service) {
        post(url) {
            expectSuccess = true
            block()
        }
    }


inline fun requestCatching(url: String, service: String, block: () -> HttpResponse): HttpResponse = try {
    block()
} catch (clientRequestException: ClientRequestException) {
    throw ServiceRequestException(service, url, clientRequestException)
} catch (serverResponseException: ServerResponseException) {
    throw ServiceRequestException(service, url, serverResponseException)
} catch (socketTimout: SocketTimeoutException) {
    throw ServiceRequestException(service, url, socketTimout, HttpStatusCode.ServiceUnavailable)
} catch (requestTimeoutException: HttpRequestTimeoutException) {
    throw ServiceRequestException(service, url, requestTimeoutException, HttpStatusCode.ServiceUnavailable)
} catch (t: Throwable) {
    throw ServiceRequestException(service, url, t, HttpStatusCode.InternalServerError)
}

inline fun requestCatching(url: String, block: () -> HttpResponse): HttpResponse =
    try {
        block()
    } catch (t: Throwable) {
        throw ApiException(t, url)
    }




