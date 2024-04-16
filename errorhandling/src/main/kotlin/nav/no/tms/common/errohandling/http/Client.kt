package nav.no.tms.common.errohandling.http

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import java.net.SocketTimeoutException


suspend fun HttpClient.getFromService(url: String, service: String, block: HttpRequestBuilder.() -> Unit = {}) = try {
    get(url) {
        expectSuccess = true
        block()
    }
} catch (clientRequestException: ClientRequestException) {
    throw ApiException(service, url, clientRequestException)
} catch (serverResponseException: ServerResponseException) {
    throw ApiException(service,url, serverResponseException)
} catch (socketTimout: SocketTimeoutException) {
    throw ApiException(service, url, socketTimout, HttpStatusCode.ServiceUnavailable)
} catch (requestTimeoutException: HttpRequestTimeoutException) {
    throw ApiException(service, url, requestTimeoutException, HttpStatusCode.ServiceUnavailable)
} catch (t: Throwable) {
    throw ApiException(service, url, t, HttpStatusCode.InternalServerError)
}



