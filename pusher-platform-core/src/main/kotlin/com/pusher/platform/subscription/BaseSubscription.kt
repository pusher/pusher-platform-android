package com.pusher.platform.subscription

import com.pusher.platform.BaseClient
import com.pusher.platform.logger.Logger
import com.pusher.platform.logger.logWith
import com.pusher.platform.network.*
import com.pusher.util.Result
import com.pusher.util.flatten
import elements.*
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.internal.http2.ErrorCode
import okhttp3.internal.http2.StreamResetException
import java.io.IOException
import java.util.concurrent.Future
import javax.net.ssl.SSLHandshakeException

internal class BaseSubscription<A>(
    path: String,
    headers: Headers,
    httpClient: OkHttpClient,
    private val onOpen: (Headers) -> Unit,
    private val onError: (Error) -> Unit,
    private val onEvent: (SubscriptionEvent<A>) -> Unit,
    private val onEnd: (EOSEvent?) -> Unit,
    private val logger: Logger,
    private val messageParser: DataParser<A>,
    val baseClient: BaseClient
): Subscription {

    private val call: Call
    private val job: Future<Unit>
    private var activeResponseBody: ResponseBody? = null

    init {
        val request = baseClient.createRequest {
            method("SUBSCRIBE", null)
            url(path.replaceMultipleSlashesInUrl())
            headers.forEach { (name, values) ->
                values.forEach { value -> addHeader(name, value) }
            }
        }

        call = httpClient.newCall(request)

        job = Futures.schedule {
            try {
                val response = call.execute()
                when (response.code()) {
                    in 200..299 -> handleConnectionOpened(response)
                    in 400..599 -> handleConnectionFailed(response)
                    else -> onError(NetworkError("Connection failed"))
                }
                response.close()
            } catch (e: IOException) {
                when {
                    call.isCanceled -> onEnd(null)
                    e is StreamResetException && e.errorCode == ErrorCode.CANCEL -> onEnd(null)
                    e is SSLHandshakeException -> onError(Errors.other(e))
                    else -> onError(NetworkError("Connection failed"))
                }
            }
        }
    }

    private fun handleConnectionFailed(response: Response) {
        val errorEvent = response.body()?.charStream()
            .parseOr { ErrorResponseBody("Could not parse: $response") }
            .logWith(logger) { verbose("") }
            .map {
                ErrorResponse(
                    statusCode = response.code(),
                    headers = response.headers().toMultimap(),
                    error = it.error,
                    errorDescription = it.errorDescription,
                    URI = it.URI
                ) as Error
            }
            .flatten()
        onError(errorEvent)
    }

    private fun handleConnectionOpened(response: Response) {
        onOpen(response.headers().toMultimap())

        val body = response.body()
        activeResponseBody = body
        when (body) {
            null -> onError(NetworkError("No response."))
            else -> body.messages
                .map { result -> result.report() }
                .any { it is EOSEvent }
                .let { ended -> if (!ended) onEnd(null) }
        }
    }

    private fun Result<SubscriptionMessage<A>, Error>.report() : SubscriptionMessage<A>? {
        when (this) {
            is Result.Failure -> onError(error)
            is Result.Success -> when (value) {
                is ControlEvent -> Unit // Ignore
                is SubscriptionEvent -> onEvent(value)
                is EOSEvent -> onEnd(value)
            }
        }
        return (this as? Result.Success)?.value
    }

    private val ResponseBody.messages: Sequence<Result<SubscriptionMessage<A>, Error>>
        get() = charStream().buffered().lineSequence()
            .map {line -> line.toSubscriptionMessage(messageParser) }

    override fun unsubscribe() {
        call.takeUnless { it.isCanceled }?.cancel()
        job.takeUnless { it.isCancelled }?.cancel()
        activeResponseBody?.close()
    }
}
