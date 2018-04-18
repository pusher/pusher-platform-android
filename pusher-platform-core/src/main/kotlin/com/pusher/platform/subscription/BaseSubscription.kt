package com.pusher.platform.subscription

import com.pusher.platform.BaseClient
import com.pusher.platform.MainThreadScheduler
import com.pusher.platform.ScheduledJob
import com.pusher.platform.Scheduler
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.nameCurrentThread
import com.pusher.platform.network.parseOr
import com.pusher.platform.network.replaceMultipleSlashesInUrl
import com.pusher.util.Result
import elements.*
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.internal.http2.ErrorCode
import okhttp3.internal.http2.StreamResetException
import okio.BufferedSource
import java.io.IOException
import javax.net.ssl.SSLHandshakeException


internal class BaseSubscription(
    path: String,
    headers: Headers,
    httpClient: OkHttpClient,
    onOpen: (Headers) -> Unit,
    onError: (Error) -> Unit,
    onEvent: (SubscriptionEvent) -> Unit,
    onEnd: (EOSEvent?) -> Unit,
    val logger: Logger,
    private val mainThread: MainThreadScheduler,
    backgroundThread: Scheduler,
    val baseClient: BaseClient
): Subscription {

    private val call: Call
    private val onOpen: (Headers) -> Unit = { headers -> mainThread.schedule { onOpen(headers) }}
    private val onError: (Error) -> Unit = { error -> mainThread.schedule { onError(error) }}
    private val onEvent: (SubscriptionEvent) -> Unit = { event -> mainThread.schedule { onEvent(event) }}
    private val onEnd: (EOSEvent?) -> Unit = { event -> mainThread.schedule { onEnd(event) }}

    private val job: ScheduledJob

    init {
        val request = baseClient.createRequest {
            method("SUBSCRIBE", null)
            url(path.replaceMultipleSlashesInUrl())
            headers.forEach { (name, values) ->
                values.forEach { value -> addHeader(name, value) }
            }
        }

        call = httpClient.newCall(request)

        job = backgroundThread.schedule {
            val nomer = nameCurrentThread(
                "${request.method()}: /${request.url().pathSegments().joinToString("/")}"
            )
            try {
                val response = call.execute()
                when (response.code()) {
                    in 200..299 -> handleConnectionOpened(response)
                    in 400..599 -> handleConnectionFailed(response)
                    else -> {
                        onError(NetworkError("Connection failed"))
                    }
                }
                response.close()
            } catch (e: IOException) {
                when {
                    call.isCanceled -> onEnd(null)
                    e is StreamResetException && e.errorCode == ErrorCode.CANCEL -> onEnd(null)
                    e is SSLHandshakeException -> onError(Errors.other(e))
                    else -> onError(NetworkError("Connection failed"))
                }
            } finally {
                nomer.restore()
            }
        }

    }

    private fun handleConnectionFailed(response: Response) {
        if (response.body() != null) {
            val errorEvent = response.body()?.charStream()
                .parseOr { ErrorResponseBody("Could not parse: $response") }
                .fold(
                    onFailure = { it },
                    onSuccess = {
                        ErrorResponse(
                            statusCode = response.code(),
                            headers = response.headers().toMultimap(),
                            error = it.error,
                            errorDescription = it.errorDescription,
                            URI = it.URI
                        )
                    }
                )
            onError(errorEvent)
        }
    }

    private fun handleConnectionOpened(response: Response) {
        onOpen(response.headers().toMultimap())

        val body = response.body()

        when (body) {
            null -> onError(NetworkError("No response."))
            else ->  body.source().messages
                .map { result -> result.report() }
                .any { it is EOSEvent }
                .let { ended -> if (!ended) { onEnd(null) } }
        }
    }

    private fun Result<SubscriptionMessage, Error>.report() : SubscriptionMessage? {
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

    private val BufferedSource.messages
        get() = generateSequence {
            takeUnless { exhausted() } ?.let { SubscriptionMessage.fromRaw(readUtf8LineStrict()) }
        }

    override fun unsubscribe() {
        if(!call.isCanceled){
            call.cancel()
        }
        job.cancel()
    }
}
