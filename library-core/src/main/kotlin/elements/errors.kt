package elements

data class ErrorResponse(val statusCode: Int, val headers: Headers, val error: String, val errorDescription: String? = null, val URI: String? = null): Error {
    override val reason: String = errorDescription ?: "ErrorResponse: $this"
}

data class ErrorResponseBody(val error: String, val errorDescription: String? = null, val URI: String? = null)

data class NetworkError(override val reason: String): Error

data class UploadError(override val reason: String): Error

data class OtherError(override val reason: String): Error

interface Error {
    val reason: String
}

object Errors {

    @JvmStatic
    fun network(message: String): Error = NetworkError(message)

}