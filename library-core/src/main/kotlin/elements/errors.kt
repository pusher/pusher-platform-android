package elements

data class ErrorResponse(val statusCode: Int, val headers: Headers, val error: String, val errorDescription: String? = null, val URI: String? = null) : Error {
    override val reason: String = errorDescription ?: "ErrorResponse: $this"
}

data class ErrorResponseBody(val error: String, val errorDescription: String? = null, val URI: String? = null)

data class NetworkError(override val reason: String) : Error

data class UploadError(override val reason: String) : Error

data class OtherError(override val reason: String) : Error

interface Error {
    val reason: String
}

object Errors {

    @JvmStatic
    fun network(message: String): Error = NetworkError(message)

    @JvmStatic
    fun response(
        statusCode: Int,
        headers: Headers,
        error: String,
        errorDescription: String? = null,
        URI: String? = null
    ) = ErrorResponse(statusCode, headers, error, errorDescription, URI)

    @JvmStatic
    fun responsebody(error: String, errorDescription: String? = null, URI: String? = null) =
        ErrorResponseBody(error, errorDescription, URI)

    @JvmStatic
    fun upload(reason: String) = UploadError(reason)

    @JvmStatic
    fun other(reason: String) = OtherError(reason)

}