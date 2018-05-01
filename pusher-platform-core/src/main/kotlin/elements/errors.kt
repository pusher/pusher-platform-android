package elements

import kotlin.Error as SystemError

data class ErrorResponse(val statusCode: Int, val headers: Headers, val error: String, val errorDescription: String? = null, val URI: String? = null) : Error {
    override val reason: String = errorDescription ?: "ErrorResponse: $this"
}

data class ErrorResponseBody(val error: String, val errorDescription: String? = null, val URI: String? = null)

data class NetworkError(override val reason: String) : Error

data class UploadError(override val reason: String) : Error

data class OtherError(override val reason: String, val exception: Throwable? = null) : Error

data class CompositeError(override val reason: String, val errors: List<Error>) : Error

data class EosError(val type: String, override val reason: String): Error

interface Error {
    val reason: String
}

fun Error.asSystemError(): ErrorAdapter = ErrorAdapter(this)

data class ErrorAdapter(val error: Error) : SystemError(error.reason)

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
    ): Error = ErrorResponse(statusCode, headers, error, errorDescription, URI)

    @JvmStatic
    fun upload(reason: String): Error = UploadError(reason)

    @JvmStatic
    fun other(reason: String): Error = OtherError(reason)

    @JvmStatic
    fun other(reason: String, throwable: Throwable): Error = OtherError(reason, throwable)

    @JvmStatic
    fun other(throwable: Throwable): Error = OtherError(throwable.message ?: "no message", throwable)

    @JvmStatic
    fun compose(errors: List<Error>): Error = CompositeError(
        "Multiple errors: \n ${errors.joinToString("\n") { it.reason }}",
        errors.toList()
    )

    @JvmStatic
    fun compose(vararg errors: Error): Error = compose(errors.asList())

}
