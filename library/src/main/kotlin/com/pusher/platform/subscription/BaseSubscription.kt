package com.pusher.platform.subscription

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.google.gson.Gson
import elements.*
import elements.Headers
import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit


class BaseSubscription(
        path: String,
        headers: Headers,
        onOpen: ( Headers ) -> Unit,
        onError: (Error) -> Unit,
        onEvent: (SubscriptionEvent) -> Unit,
        onEnd: (EOSEvent?) -> Unit
): Subscription {

    private val client= OkHttpClient.Builder().readTimeout(0, TimeUnit.MINUTES).build()
    private val call: Call
    private var response: Response? = null

    companion object {
        val GSON = Gson()
    }

    //Call must be executed in a background thread, otherwise stupid OKHTTP doesn't propagate connection dead events. Sad.
    private val subscriptionThread: Thread
    private val mainThread = Handler(Looper.getMainLooper())
    private val onOpen: (Headers) -> Unit = { headers -> mainThread.post{ onOpen(headers) }}
    private val onError: (Error) -> Unit = { error -> mainThread.post{ onError(error) }}
    private val onEvent: (SubscriptionEvent) -> Unit = { event -> mainThread.post { onEvent(event) }}
    private val onEnd: (EOSEvent?) -> Unit = { event -> mainThread.post { onEnd(event) }}

    init {
        var requestBuilder = Request.Builder()
                .method("SUBSCRIBE", null)
                .url(path)

        headers.entries.forEach { entry -> entry.value.forEach { requestBuilder.addHeader(entry.key, it) } }
        val request = requestBuilder.build()

        call = client.newCall(request)

        subscriptionThread = object : HandlerThread("BaseSubscription", android.os.Process.THREAD_PRIORITY_BACKGROUND) {

            override fun run() {
                try {
                    val response = call.execute()
                    this@BaseSubscription.response = response

                    when (response.code()) {
                        in 200..299 -> handleConnectionOpened(response)
                        in 400..599 -> handleConnectionFailed(response)
                        else -> {
                            mainThread.post {
                                onError(NetworkError("Connection failed"))
                            }
                        }
                    }
                } catch (e: IOException) {
                    onError(NetworkError("Connection failed"))
                    interrupt()
                }
            }

            override fun interrupt() {
                super.interrupt()
                response?.close()
            }
        }
        subscriptionThread.start()
    }

    private fun handleConnectionFailed(response: Response) {
        if(response.body() != null){
            val body = GSON.fromJson(response.body()!!.charStream(), ErrorResponseBody::class.java)

            mainThread.post {
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
        if(subscriptionThread.isAlive){
            subscriptionThread.interrupt()
        }
    }
}

