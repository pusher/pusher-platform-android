package com.pusher.platform.subscription

import com.pusher.platform.BaseClient.Companion.GSON
import com.pusher.platform.MainThreadScheduler
import com.pusher.platform.ScheduledJob
import com.pusher.platform.Scheduler
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.nameCurrentThread
import com.pusher.platform.network.replaceMultipleSlashesInUrl
import elements.*
import elements.Headers
import okhttp3.*
import okhttp3.internal.http2.ErrorCode
import okhttp3.internal.http2.StreamResetException
import java.io.IOException


class BaseSubscription(
        path: String,
        headers: Headers,
        httpClient: OkHttpClient,
        onOpen: ( Headers ) -> Unit,
        onError: (Error) -> Unit,
        onEvent: (SubscriptionEvent) -> Unit,
        onEnd: (EOSEvent?) -> Unit,
        val logger: Logger,
        private val mainThread: MainThreadScheduler,
        backgroundThread: Scheduler
): Subscription {

    private val call: Call
    private val onOpen: (Headers) -> Unit = { headers -> mainThread.schedule { onOpen(headers) }}
    private val onError: (Error) -> Unit = { error -> mainThread.schedule { onError(error) }}
    private val onEvent: (SubscriptionEvent) -> Unit = { event -> mainThread.schedule { onEvent(event) }}
    private val onEnd: (EOSEvent?) -> Unit = { event -> mainThread.schedule { onEnd(event) }}

    private val job: ScheduledJob

    init {
        val requestBuilder = Request.Builder()
                .method("SUBSCRIBE", null)
                .url(path.replaceMultipleSlashesInUrl())

        headers.entries.forEach { entry -> entry.value.forEach { requestBuilder.addHeader(entry.key, it) } }
        val request = requestBuilder.build()

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
                if(e is StreamResetException && e.errorCode == ErrorCode.CANCEL){
                    onEnd(null)
                } else{
                    onError(NetworkError("Connection failed"))
                }
            } finally {
                nomer.restore()
            }
        }

    }

    private fun handleConnectionFailed(response: Response) {
        if(response.body() != null){
            val body = GSON.fromJson(response.body()!!.charStream(), ErrorResponseBody::class.java)

            mainThread.schedule {
                onError(ErrorResponse(
                        statusCode = response.code(),
                        headers = response.headers().toMultimap(),
                        error = body.error,
                        errorDescription = body.errorDescription,
                        URI = body.URI
                ))
            }
        }
    }

    private fun handleConnectionOpened(response: Response) {
        onOpen(response.headers().toMultimap())

        if (response.body() != null) {
            while (!response.body()!!.source().exhausted()) {
                val messageString = response.body()!!.source().readUtf8LineStrict()
                val event = SubscriptionMessage.fromRaw(messageString)
                logger.verbose("${BaseSubscription@this} received event: $event")
                when (event) {
                    is ControlEvent -> {} // Ignore
                    is SubscriptionEvent -> {
                        onEvent(event)
                    }
                    is EOSEvent -> {
                        onEnd(event)
                    }
                }
            }
        }
        else{
            onError(NetworkError("No response."))
        }
    }

    override fun unsubscribe() {
        if(!call.isCanceled){
            call.cancel()
        }
        job.cancel()
    }
}
