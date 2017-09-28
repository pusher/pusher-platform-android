package com.pusher.platform

import android.content.Context
import com.google.gson.Gson
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.ConnectivityHelper
import com.pusher.platform.retrying.RetryStrategyOptions
import com.pusher.platform.subscription.BaseSubscription
import com.pusher.platform.subscription.SubscribeStrategy
import com.pusher.platform.subscription.createResumingStrategy
import com.pusher.platform.subscription.createTokenProvidingStrategy
import com.pusher.platform.tokenProvider.TokenProvider
import elements.*
import elements.Headers
import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit

class BaseClient(
        var host: String,
        val logger: Logger,
        encrypted: Boolean = true,
        val context: Context) {

        val prefix = if(encrypted) "https" else "http"
        val baseUrl = "$prefix://$host"

        val httpClient = OkHttpClient.Builder().readTimeout(0, TimeUnit.MINUTES).build()

    companion object {
        val GSON = Gson()
    }

    fun subscribeResuming(
            path: String,
            listeners: SubscriptionListeners,
            headers: Headers,
            tokenProvider: TokenProvider?,
            retryOptions: RetryStrategyOptions
    ): Subscription {

        val subscribeStrategy: SubscribeStrategy = createResumingStrategy(
                initialEventId = "",
                logger = logger,
                nextSubscribeStrategy = createTokenProvidingStrategy(
                        tokenProvider = tokenProvider,
                        logger = logger,
                        nextSubscribeStrategy = createBaseSubscription(path = absolutePath(path))),
                errorResolver = ErrorResolver(ConnectivityHelper(context), retryOptions)
        )

        return subscribeStrategy(listeners, headers)
    }

    fun subscribeNonResuming(): Subscription {
        TODO()
    }

    fun request(
            path: String,
            headers: elements.Headers,
            method: String,
            body: String? = null,
            tokenProvider: TokenProvider? = null,
            tokenParams: Any? = null,
            onSuccess: (Response) -> Unit,
            onFailure: (elements.Error) -> Unit): Cancelable {

        val requestBody = if (body != null) {
            RequestBody.create(MediaType.parse("application/json"), body)
        } else null

        val requestBuilder = Request.Builder()
                .method(method, requestBody)
                .url("$baseUrl/$path")

        headers.entries.forEach { entry -> entry.value.forEach { requestBuilder.addHeader(entry.key, it) } }


        return object: Cancelable {

            val call: Call = httpClient.newCall(requestBuilder.build())

            override fun cancel() {
                if(!call.isCanceled) call.cancel()
            }

            init {
                call.enqueue(object : Callback {
                    override fun onResponse(call: Call?, response: Response?) {
                        if(response != null){
                            when(response?.code()){
                                in 200..299 -> onSuccess(response)
                                else -> {
                                    val errorBody = GSON.fromJson(response.body()!!.string(), ErrorResponseBody::class.java)
                                    onFailure(ErrorResponse(
                                            statusCode = response.code(),
                                            headers = response.headers().toMultimap(),
                                            error = errorBody.error,
                                            errorDescription = errorBody.errorDescription,
                                            URI = errorBody.URI
                                    ))
                                }
                            }

                        }

                    }

                    override fun onFailure(call: Call?, e: IOException?) {
                        onFailure(NetworkError("Network error"))
                    }
                })
            }
        }
    }

    fun createBaseSubscription(path: String): SubscribeStrategy {
        return { listeners, headers ->
            BaseSubscription(
                    path = path,
                    headers = headers,
                    onOpen = listeners.onOpen,
                    onError = listeners.onError,
                    onEvent = listeners.onEvent,
                    onEnd = listeners.onEnd,
                    httpClient = httpClient
            )
        }
    }


private  fun absolutePath(path: String): String = "$baseUrl/$path"
}





