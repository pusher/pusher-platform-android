package elements

data class ErrorResponse(val statusCode: Int, val headers: Headers, val error: String, val errorDescription: String? = null, val URI: String? = null): Error()

data class ErrorResponseBody(val error: String, val errorDescription: String? = null, val URI: String? = null)

data class NetworkError(val reason: String): Error()

data class UploadError(val reason: String): Error()

data class OtherError(val reason: String): Error()

sealed class Error