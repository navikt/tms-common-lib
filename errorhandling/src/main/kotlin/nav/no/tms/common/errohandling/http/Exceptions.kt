package nav.no.tms.common.errohandling.http


class ApiException(val service: String, val url: String, originalException: Exception) : Throwable() {

}

