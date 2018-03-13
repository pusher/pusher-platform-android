package com.pusher.platform

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.pusher.network.Promise
import com.pusher.network.asPromise
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.ConnectivityHelper
import com.pusher.platform.network.OkHttpResponsePromise
import com.pusher.platform.network.replaceMultipleSlashesInUrl
import com.pusher.platform.retrying.RetryStrategyOptions
import com.pusher.platform.subscription.*
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.Result
import com.pusher.util.asFailure
import com.pusher.util.asSuccess
import elements.*
import elements.Headers
import okhttp3.*
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

class BaseClient(
    var host: String,
    internal val logger: Logger,
    internal val connectivityHelper: ConnectivityHelper,
    internal val mediaTypeResolver: MediaTypeResolver,
    internal val scheduler: Scheduler,
    internal val mainScheduler: MainThreadScheduler,
    encrypted: Boolean = true
) {

    val prefix = if (encrypted) "https" else "http"
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
            errorResolver = ErrorResolver(connectivityHelper, retryOptions, scheduler)
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
            errorResolver = ErrorResolver(connectivityHelper, retryOptions, scheduler)
        )
        return subscribeStrategy(listeners, headers)
    }

    fun request(
        requestDestination: RequestDestination,
        headers: elements.Headers,
        method: String,
        body: String? = null,
        tokenProvider: TokenProvider? = null,
        tokenParams: Any? = null
    ): OkHttpResponsePromise {
        val requestBody = body?.let { RequestBody.create(MediaType.parse("application/json"), it) }
        return when(tokenProvider) {
            null -> performRequest(requestDestination, headers, method, requestBody)
            else -> tokenProvider.fetchToken(tokenParams).flatMap { token ->
                headers["Authorization"] = listOf("Bearer $token")
                performRequest(requestDestination, headers, method, requestBody)
            }
        }
    }

    fun upload(
        requestDestination: RequestDestination,
        headers: elements.Headers = TreeMap(),
        file: File,
        tokenProvider: TokenProvider? = null,
        tokenParams: Any? = null
    ): OkHttpResponsePromise = when {
        file.exists() -> {
            val mediaType = MediaType.parse(mediaTypeResolver.fileMediaType(file) ?: "")
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, RequestBody.create(mediaType, file))
                .build()

            when(tokenProvider) {
                null -> performRequest(requestDestination, headers, "POST", requestBody)
                else -> tokenProvider.fetchToken(tokenParams).flatMap { token ->
                    headers["Authorization"] = listOf("Bearer $token")
                    performRequest(requestDestination, headers, "POST", requestBody)
                }
            }
        }
        else -> UploadError("File does not exist at ${file.path}").asFailure<Response, Error>().asPromise()
    }

    fun TokenProvider.fetchToken(tokenParams: Any? = null): Promise<Result<String, elements.Error>> = Promise.promise {
        // TODO: convert tokenProvider to promises
        fetchToken(tokenParams,
            { report(it.asSuccess()) },
            { report(it.asFailure()) }
        )
    }

    private fun performRequest(
        requestDestination: RequestDestination,
        headers: Headers,
        method: String,
        requestBody: RequestBody?
    ): OkHttpResponsePromise = Promise.promise {
        val requestURL = getRequestPath(requestDestination)

        val requestBuilder = Request.Builder()
            .method(method, requestBody)
            .url(requestURL)

        headers.entries.forEach { entry -> entry.value.forEach { requestBuilder.addHeader(entry.key, it) } }

        val call: Call = httpClient.newCall(requestBuilder.build())

        onCancel { if (!call.isCanceled) call.cancel() }

        call.enqueue(object : Callback {
            override fun onResponse(call: Call?, response: Response?) {
                when (response?.code()) {
                    null -> report(OtherError("Response was null").asFailure())
                    in 200..299 -> report(response.asSuccess())
                    else -> {
                        val errorBody = GSON.fromJson(response.body()!!.string(), ErrorResponseBody::class.java)
                        report(ErrorResponse(
                            statusCode = response.code(),
                            headers = response.headers().toMultimap(),
                            error = errorBody.error,
                            errorDescription = errorBody.errorDescription,
                            URI = errorBody.URI
                        ).asFailure())
                    }
                }
            }

            override fun onFailure(call: Call?, e: IOException?) {
                report(NetworkError("Request error: ${e?.toString()}").asFailure())
            }
        })
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
                logger = logger,
                mainThread = mainScheduler,
                backgroundThread = scheduler
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
