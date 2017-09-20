package elements

data class ErrorResponse(val statusCode: Int, val headers: Headers, val error: String, val errorDescription: String?, val URI: String?): Error()

data class ErrorResponseBody(val error: String, val errorDescription: String? = null, val URI: String? = null)

data class NetworkError(val reason: String): Error()


sealed class Error