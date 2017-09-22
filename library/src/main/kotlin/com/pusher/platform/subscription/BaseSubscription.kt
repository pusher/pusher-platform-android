package com.pusher.platform.subscription

import com.google.gson.Gson
import elements.*
import elements.Headers
import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit


class BaseSubscription(

        path: String,
        headers: Headers,
        val onOpen: ( Headers ) -> Unit,
        val onError: (Error ) -> Unit,
        val onEvent: (SubscriptionEvent) -> Unit,
        val onEnd: (EOSEvent?) -> Unit
): Subscription {

    private val client= OkHttpClient.Builder().readTimeout(0, TimeUnit.MINUTES).build()
    private val call: Call
    private lateinit var response: Response

    companion object {
        val GSON = Gson()
    }


    init {
        var requestBuilder = Request.Builder()
                .method("SUBSCRIBE", null)
                .url(path)

        headers.entries.forEach { entry -> entry.value.forEach { requestBuilder.addHeader(entry.key, it) } }
        val request = requestBuilder.build()

        call = client.newCall(request)
        try{
            call.enqueue(object: Callback {
                override fun onFailure(call: Call?, e: IOException?) {
                    onError(NetworkError("Connection failed"))
                }

                override fun onResponse(call: Call?, response: Response?) {
                    if (response != null) {
                        this@BaseSubscription.response = response

                        when (response.code()){
                            in 200..299 -> handleConnectionOpened(response)
                            in 400..599 -> handleConnectionFailed(response)
                            else -> onError(NetworkError("Connection failed"))
                        }
                    }
                    else {
                        onError(NetworkError("Connection failed"))
                    }
                }
            })


        }
        catch (e: IOException){
            onError(NetworkError("Connection failed"))
        }
    }

    private fun handleConnectionFailed(response: Response) {
        if(response.body() != null){
            val body = GSON.fromJson(response.body()!!.charStream(), ErrorResponseBody::class.java)

            onError(ErrorResponse(
                    statusCode = response.code(),
                    headers = response.headers().toMultimap(),
                    error = body.error,
                    errorDescription = body.errorDescription,
                    URI = body.URI
            ))
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
                    is SubscriptionEvent -> onEvent(event)
                    is EOSEvent -> onEnd(event)
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
        response.close()
    }
}

