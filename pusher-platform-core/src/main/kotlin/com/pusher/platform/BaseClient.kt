package com.pusher.platform

import com.pusher.platform.network.*
import com.pusher.platform.retrying.RetryStrategyOptions
import com.pusher.platform.subscription.*
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.asFailure
import com.pusher.util.asSuccess
import elements.*
import elements.Headers
import okhttp3.*
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

data class BaseClient(
    val host: String,
    val dependencies: PlatformDependencies,
    val client: OkHttpClient = OkHttpClient(),
    val encrypted: Boolean = true
) {

    private val schema = if (encrypted) "https" else "http"
    private val baseUrl = "$schema://$host"

    internal val logger = dependencies.logger
    private val scheduler = dependencies.scheduler
    private val mainScheduler = dependencies.mainScheduler
    private val mediaTypeResolver = dependencies.mediaTypeResolver
    private val connectivityHelper = dependencies.connectivityHelper
    private val sdkInfo = dependencies.sdkInfo

    private val httpClient = client.newBuilder().apply {
        readTimeout(0, TimeUnit.MINUTES)
    }.build()

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
        return when (tokenProvider) {
            null -> performRequest(requestDestination, headers, method, requestBody)
            else -> tokenProvider.fetchToken(tokenParams).flatMap {
                it.map { token ->
                    val authHeaders = headers + ("Authorization" to listOf("Bearer $token"))
                    performRequest(requestDestination, authHeaders, method, requestBody)
                }.recover {
                    it.asFailure<Response, Error>().asPromise()
                }
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

            when (tokenProvider) {
                null -> performRequest(requestDestination, headers, "POST", requestBody)
                else -> tokenProvider.fetchToken(tokenParams).flatMap { token ->
                    val authHeaders = headers + ("Authorization" to listOf("Bearer $token"))
                    performRequest(requestDestination, authHeaders, "POST", requestBody)
                }
            }
        }
        else -> UploadError("File does not exist at ${file.path}").asFailure<Response, Error>().asPromise()
    }

    /**
     * Ensures that:
     *  - GET doesn't have a body
     *  - PUT and POST have an empty body if missing
     */
    private fun RequestBody?.forMethod(method: String): RequestBody? = when(method.toUpperCase()) {
        "GET" -> null
        "POST", "PUT" -> this ?: RequestBody.create(MediaType.parse("text/plain"), "")
        else -> this
    }

    private fun performRequest(
        requestDestination: RequestDestination,
        headers: Headers,
        method: String,
        requestBody: RequestBody?
    ): OkHttpResponsePromise = Promise.promise {
        val requestURL = getRequestPath(requestDestination)

        val request = createRequest {
            method(method, requestBody.forMethod(method))
            url(requestURL)
            headers.forEach { (name, values) ->
                values.forEach { value -> addHeader(name, value) }
            }
        }

        val call: Call = httpClient.newCall(request)

        onCancel { if (!call.isCanceled) call.cancel() }

        call.enqueue(object : Callback {
            override fun onResponse(call: Call?, response: Response?) {
                when (response?.code()) {
                    null -> report(OtherError("Response was null").asFailure())
                    in 200..299 -> report(response.asSuccess())
                    else -> {
                        val error = response.body()?.charStream()
                            .parseOr { ErrorResponseBody("could not parse error response: $response") }
                            .map { b ->
                                Errors.response(
                                    statusCode = response.code(),
                                    headers = response.headers().toMultimap(),
                                    error = b.error,
                                    errorDescription = b.errorDescription,
                                    URI = b.URI
                                )
                            }.recover { it }
                        report(error.asFailure())
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
                backgroundThread = scheduler,
                baseClient = this
            )
        }
    }

    internal fun createRequest(block: Request.Builder.() -> Unit): Request =
        Request.Builder().apply {
            addHeader("X-SDK-Product", sdkInfo.product)
            addHeader("X-SDK-Version", sdkInfo.sdkVersion)
            addHeader("X-SDK-Language", sdkInfo.language)
            addHeader("X-SDK-Platform", sdkInfo.platform)
        }.also(block).build()

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
