package com.pusher.platform

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.ConnectivityHelper
import com.pusher.platform.network.replaceMultipleSlashesInUrl
import com.pusher.platform.retrying.RetryStrategyOptions
import com.pusher.platform.subscription.*
import com.pusher.platform.tokenProvider.TokenProvider
import elements.*
import elements.Headers
import okhttp3.*
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import okhttp3.RequestBody
import okhttp3.MultipartBody


class BaseClient(
        var host: String,
        val logger: Logger,
        encrypted: Boolean = true,
        val context: Context) {

        val prefix = if(encrypted) "https" else "http"
        val baseUrl = "$prefix://$host"

        val httpClient: okhttp3.OkHttpClient = OkHttpClient.Builder().readTimeout(0, TimeUnit.MINUTES).build()

    companion object {
        val GSON = GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create()
    }

    fun subscribeResuming(
            requestDestination: RequestDestination,
            listeners: SubscriptionListeners,
            headers: Headers,
            tokenProvider: TokenProvider?,
            tokenParams: Any?,
            retryOptions: RetryStrategyOptions,
            initialEventId: String? = null
    ): Subscription {
        val subscribeStrategy: SubscribeStrategy = createResumingStrategy(
                initialEventId = initialEventId,
                logger = logger,
                nextSubscribeStrategy = createTokenProvidingStrategy(
                        tokenProvider = tokenProvider,
                        tokenParams = tokenParams,
                        logger = logger,
                        nextSubscribeStrategy = createBaseSubscription(
                                path = getRequestPath(requestDestination)
                        )
                ),
                errorResolver = ErrorResolver(ConnectivityHelper(context), retryOptions)
        )

        return subscribeStrategy(listeners, headers)
    }

    fun subscribeNonResuming(
            requestDestination: RequestDestination,
            listeners: SubscriptionListeners,
            headers: Headers,
            tokenProvider: TokenProvider?,
            tokenParams: Any?,
            retryOptions: RetryStrategyOptions): Subscription {

        val subscribeStrategy: SubscribeStrategy = createRetryingStrategy(
                logger = logger,
                nextSubscribeStrategy = createTokenProvidingStrategy(
                        tokenProvider = tokenProvider,
                        tokenParams = tokenParams,
                        logger = logger,
                        nextSubscribeStrategy = createBaseSubscription(path = getRequestPath(requestDestination))),
                errorResolver = ErrorResolver(ConnectivityHelper(context), retryOptions)
        )
        return subscribeStrategy(listeners, headers)
    }

    fun request(
            requestDestination: RequestDestination,
            headers: elements.Headers,
            method: String,
            body: String? = null,
            tokenProvider: TokenProvider? = null,
            tokenParams: Any? = null,
            onSuccess: (Response) -> Unit,
            onFailure: (elements.Error) -> Unit): Cancelable {

        var requestBeingPerformed: Cancelable? = null
        val requestBody = if (body != null) {
            RequestBody.create(MediaType.parse("application/json"), body)
        } else null

        if(tokenProvider != null) {
            requestBeingPerformed = tokenProvider.fetchToken(
                    tokenParams = tokenParams,
                    onFailure = onFailure,
                    onSuccess = { token ->
                        headers.put("Authorization", listOf("Bearer $token"))
                        requestBeingPerformed = performRequest(requestDestination, headers, method, requestBody, onSuccess, onFailure)
                    }
            )
        }
        else {
            requestBeingPerformed = performRequest(requestDestination, headers, method, requestBody, onSuccess, onFailure)
        }

        return object: Cancelable {
            override fun cancel() {
                requestBeingPerformed?.cancel()
            }
        }
    }

    fun upload(
            requestDestination: RequestDestination,
            headers: elements.Headers = TreeMap(),
            file: File,
            tokenProvider: TokenProvider? = null,
            tokenParams: Any? = null,
            onSuccess: (Response) -> Unit,
            onFailure: (Error) -> Unit): Cancelable? {

        if (!file.exists()) {
            onFailure(UploadError("File does not exist at ${file.path}"))
            return null
        }

        var requestBeingPerformed: Cancelable? = null
        val mediaType = MediaType.parse(
                MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(
                                MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString())
                        )
        )

        val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, RequestBody.create(mediaType, file))
                .build()


        if (tokenProvider != null) {
            requestBeingPerformed = tokenProvider.fetchToken(
                    tokenParams = tokenParams,
                    onFailure = onFailure,
                    onSuccess = { token ->
                        headers.put("Authorization", listOf("Bearer $token"))
                        requestBeingPerformed = performRequest(requestDestination, headers, "POST", requestBody, onSuccess, onFailure)
                    }
            )
        } else {
            requestBeingPerformed = performRequest(requestDestination, headers, "POST", requestBody, onSuccess, onFailure)
        }

        return object: Cancelable {
            override fun cancel() {
                requestBeingPerformed?.cancel()
            }
        }
    }

    private fun performRequest(requestDestination: RequestDestination, headers: Headers, method: String, requestBody: RequestBody?, onSuccess: (Response) -> Unit, onFailure: (Error) -> Unit): Cancelable {
        val requestURL = getRequestPath(requestDestination)

        val requestBuilder = Request.Builder()
                .method(method, requestBody)
                .url(requestURL)

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
                        onFailure(NetworkError("Request error: ${e?.toString()}"))
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
                    httpClient = httpClient,
                    logger = logger
            )
        }
    }

    private fun getRequestPath(requestDestination: RequestDestination): String {
        return when (requestDestination) {
            is RequestDestination.Absolute -> {
                requestDestination.url
            }
            is RequestDestination.Relative -> {
                absolutePath(requestDestination.path)
            }
        }
    }

    private fun absolutePath(path: String): String = "$baseUrl/$path".replaceMultipleSlashesInUrl()
}
