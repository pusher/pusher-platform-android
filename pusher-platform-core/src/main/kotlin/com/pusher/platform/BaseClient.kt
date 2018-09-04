package com.pusher.platform

import com.pusher.platform.RequestDestination.Absolute
import com.pusher.platform.RequestDestination.Relative
import com.pusher.platform.logger.log
import com.pusher.platform.network.*
import com.pusher.platform.retrying.RetryStrategyOptions
import com.pusher.platform.subscription.*
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.*
import elements.*
import okhttp3.*
import java.io.File
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import elements.Headers as ElementsHeaders

data class BaseClient(
    val host: String,
    val dependencies: PlatformDependencies,
    val client: OkHttpClient = OkHttpClient(),
    private val encrypted: Boolean = true
) {

    private val schema = if (encrypted) "https" else "http"
    private val baseUrl = "$schema://$host"

    internal val logger = dependencies.logger
    private val mediaTypeResolver = dependencies.mediaTypeResolver
    private val sdkInfo = dependencies.sdkInfo

    private val httpClient = client.newBuilder().apply {
        readTimeout(0, TimeUnit.MINUTES)
    }.build()

    @JvmOverloads
    fun <A> subscribeResuming(
            destination: RequestDestination,
            listeners: SubscriptionListeners<A>,
            headers: ElementsHeaders,
            tokenProvider: TokenProvider?,
            tokenParams: Any?,
            retryOptions: RetryStrategyOptions,
            bodyParser: DataParser<A>,
            initialEventId: String? = null
    ): Subscription {
        val id = SubscriptionIDGenerator.next()
        return createResumingStrategy(
                subscriptionID = id,
                initialEventId = initialEventId,
                logger = logger,
                nextSubscribeStrategy = createTokenProvidingStrategy(
                        subscriptionID = id,
                        tokenProvider = tokenProvider,
                        tokenParams = tokenParams,
                        logger = logger,
                        nextSubscribeStrategy = createBaseSubscription(
                                path = destination.toRequestPath(),
                                bodyParser = bodyParser
                        )
                ),
                errorResolver = ErrorResolver(retryOptions)
        )(listeners, headers)
    }

    fun <A> subscribeNonResuming(
            destination: RequestDestination,
            listeners: SubscriptionListeners<A>,
            headers: ElementsHeaders,
            tokenProvider: TokenProvider?,
            tokenParams: Any?,
            retryOptions: RetryStrategyOptions,
            bodyParser: DataParser<A>
    ): Subscription {
        val id = SubscriptionIDGenerator.next()
        return createRetryingStrategy(
                subscriptionID = id,
                logger = logger,
                nextSubscribeStrategy = createTokenProvidingStrategy(
                        subscriptionID = id,
                        tokenProvider = tokenProvider,
                        tokenParams = tokenParams,
                        logger = logger,
                        nextSubscribeStrategy = createBaseSubscription(path = destination.toRequestPath(), bodyParser = bodyParser)),
                errorResolver = ErrorResolver(retryOptions)
        )(listeners, headers)
    }

    @JvmOverloads
    fun <A> request(
            requestDestination: RequestDestination,
            headers: ElementsHeaders,
            method: String,
            responseParser: DataParser<A>,
            body: String? = null,
            tokenProvider: TokenProvider? = null,
            tokenParams: Any? = null
    ): Future<Result<A, Error>> = tokenProvider
            .authHeaders(headers, tokenParams)
            .flatMapFutureResult { authHeaders ->
                performRequest(
                        destination = requestDestination,
                        headers = authHeaders,
                        method = method,
                        requestBody = body?.let { RequestBody.create(MediaType.parse("application/json"), it) },
                        responseParser = responseParser
                )
            }

    @JvmOverloads
    fun <A> upload(
            requestDestination: RequestDestination,
            headers: ElementsHeaders = emptyHeaders(),
            file: File,
            responseParser: DataParser<A>,
            tokenProvider: TokenProvider? = null,
            tokenParams: Any? = null
    ): Future<Result<A, Error>> = when {
        file.exists() -> {
            tokenProvider.authHeaders(headers, tokenParams).flatMapFutureResult { authHeaders ->
                performRequest(
                        destination = requestDestination,
                        headers = authHeaders,
                        method = "POST",
                        requestBody = file.toRequestMultipartBody(),
                        responseParser = responseParser
                )
            }
        }
        else -> Futures.now(Errors.upload("File does not exist at ${file.path}").asFailure())
    }

    /**
     * Provides a future that will provide the same headers with auth token if possible.
     */
    private fun TokenProvider?.authHeaders(headers: ElementsHeaders, tokenParams: Any? = null): Future<Result<ElementsHeaders, Error>> =
            this?.fetchToken(tokenParams)
                    ?.mapResult { token -> headers + ("Authorization" to listOf("Bearer $token")) }
                    ?: headers.asSuccess<ElementsHeaders, Error>().toFuture()

    private fun File.toRequestMultipartBody(): MultipartBody =
            MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", name, toRequestBody())
                    .build()

    private fun File.toRequestBody(): RequestBody =
            RequestBody.create(MediaType.parse(mediaTypeResolver.fileMediaType(this) ?: ""), this)

    /**
     * Ensures that:
     *  - GET doesn't have a body
     *  - PUT and POST have an empty body if missing
     */
    private fun RequestBody?.forMethod(method: String): RequestBody? = when (method.toUpperCase()) {
        "GET" -> null
        "POST", "PUT" -> this ?: RequestBody.create(MediaType.parse("text/plain"), "")
        else -> this
    }

    private fun <A> performRequest(
            destination: RequestDestination,
            headers: ElementsHeaders,
            method: String,
            requestBody: RequestBody?,
            responseParser: DataParser<A>
    ): Future<Result<A, Error>> = Futures.schedule {
        val requestURL = destination.toRequestPath()
        logger.verbose("Request started: $method $requestURL with body: $requestBody")

        val request = createRequest {
            method(method, requestBody.forMethod(method))
            url(requestURL)
            headers.forEach { (name, values) ->
                values.forEach { value -> addHeader(name, value) }
            }
        }

        val response = httpClient.newCall(request).execute()

        when (response?.code()) {
            null -> OtherError("Response was null").asFailure<A, Error>()
            in 200..299 -> logger
                    .log(response) { verbose("Request OK: $method $requestURL with status code: ${response.code()} ") }
                    .body()?.string()
                    .orElse { Errors.other("Missing body in $response") }
                    .flatMap(responseParser)
            else -> logger
                    .log(response) { verbose("Request Failed: $method $requestURL with status code: ${response.code()}") }
                    .body()?.string()
                    .parseAs<ErrorResponse> { Errors.network("could not parse error response: $response") }
                    .map { b ->
                        Errors.response(
                                statusCode = response.code(),
                                headers = response.headers().toMultimap(),
                                error = b.error,
                                errorDescription = b.errorDescription,
                                URI = b.URI
                        )
                    }
                    .recover { it }
                    .asFailure()
        }
    }

    private fun <A> createBaseSubscription(
            path: String,
            bodyParser: DataParser<A>
    ): SubscribeStrategy<A> = { listeners, headers ->
        BaseSubscription(
                path = path,
                headers = headers,
                onOpen = listeners.onOpen,
                onError = listeners.onError,
                onEvent = listeners.onEvent,
                onEnd = listeners.onEnd,
                httpClient = httpClient,
                logger = logger,
                baseClient = this,
                messageParser = bodyParser
        )
    }

    internal fun createRequest(block: Request.Builder.() -> Unit): Request =
            Request.Builder().apply {
                addHeader("X-SDK-Product", sdkInfo.product)
                addHeader("X-SDK-Version", sdkInfo.sdkVersion)
                addHeader("X-SDK-Language", sdkInfo.language)
                addHeader("X-SDK-Platform", sdkInfo.platform)
            }.also(block).build()

    private fun RequestDestination.toRequestPath(): String = when (this) {
        is Absolute -> url
        is Relative -> absolutePath(path)
    }

    private fun absolutePath(path: String): String = "$baseUrl/$path".replaceMultipleSlashesInUrl()
}