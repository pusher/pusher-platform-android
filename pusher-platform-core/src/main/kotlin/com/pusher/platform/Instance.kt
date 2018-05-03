package com.pusher.platform

import com.pusher.platform.RequestDestination.Absolute
import com.pusher.platform.RequestDestination.Relative
import com.pusher.platform.network.DataParser
import com.pusher.platform.retrying.RetryStrategyOptions
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.Result
import elements.*
import java.io.File
import java.util.*
import java.util.concurrent.Future

private const val DEFAULT_HOST_BASE = "pusherplatform.io"

data class Instance constructor(
    private val id: String,
    val baseClient: BaseClient,
    private val serviceName: String,
    private val serviceVersion: String,
    private val instanceTokenProvider: TokenProvider? = null,
    private val instanceTokenParams: Any? = null
) {

    @JvmOverloads
    constructor(
        locator: String,
        serviceName: String,
        serviceVersion: String,
        dependencies: PlatformDependencies,
        host: String = "${Locator(locator).cluster}.$DEFAULT_HOST_BASE",
        baseClient: BaseClient = BaseClient(host, dependencies)
    ) : this(
        Locator(locator).id,
        baseClient,
        serviceName,
        serviceVersion
    )

    @JvmOverloads
    fun <A> subscribeResuming(
        path: String,
        listeners: SubscriptionListeners<A>,
        messageParser: DataParser<A>,
        headers: Headers = emptyHeaders(),
        tokenProvider: TokenProvider? = instanceTokenProvider,
        tokenParams: Any? = instanceTokenParams,
        retryOptions: RetryStrategyOptions = RetryStrategyOptions(),
        initialEventId: String? = null
    ): Subscription = subscribeResuming(
        requestDestination = Relative(path),
        listeners = listeners,
        headers = headers,
        tokenProvider = tokenProvider,
        tokenParams = tokenParams,
        retryOptions = retryOptions,
        initialEventId = initialEventId,
        messageParser = messageParser
    )

    @Suppress("MemberVisibilityCanBePrivate") // Public API
    @JvmOverloads
    fun <A> subscribeResuming(
        requestDestination: RequestDestination,
        listeners: SubscriptionListeners<A>,
        messageParser: DataParser<A>,
        headers: Headers = TreeMap(String.CASE_INSENSITIVE_ORDER),
        tokenProvider: TokenProvider? = instanceTokenProvider,
        tokenParams: Any? = instanceTokenParams,
        retryOptions: RetryStrategyOptions = RetryStrategyOptions(),
        initialEventId: String? = null
    ): Subscription = baseClient.subscribeResuming(
        destination = requestDestination.toScopedDestination(),
        listeners = listeners,
        headers = headers,
        tokenProvider = tokenProvider,
        tokenParams = tokenParams,
        retryOptions = retryOptions,
        initialEventId = initialEventId,
        bodyParser = messageParser
    )

    @JvmOverloads
    fun <A> subscribeNonResuming(
        path: String,
        listeners: SubscriptionListeners<A>,
        messageParser: DataParser<A>,
        headers: Headers = emptyHeaders(),
        tokenProvider: TokenProvider? = instanceTokenProvider,
        tokenParams: Any? = instanceTokenParams,
        retryOptions: RetryStrategyOptions = RetryStrategyOptions()
    ): Subscription = subscribeNonResuming(
        requestDestination = Relative(path),
        listeners = listeners,
        messageParser = messageParser,
        headers = headers,
        tokenProvider = tokenProvider,
        tokenParams = tokenParams,
        retryOptions = retryOptions
    )

    @Suppress("MemberVisibilityCanBePrivate") // Public API
    @JvmOverloads
    fun <A> subscribeNonResuming(
        requestDestination: RequestDestination,
        listeners: SubscriptionListeners<A>,
        headers: Headers = emptyHeaders(),
        messageParser: DataParser<A>,
        tokenProvider: TokenProvider? = instanceTokenProvider,
        tokenParams: Any? = instanceTokenParams,
        retryOptions: RetryStrategyOptions = RetryStrategyOptions()
    ): Subscription = baseClient.subscribeNonResuming(
        destination = requestDestination.toScopedDestination(),
        listeners = listeners,
        bodyParser = messageParser,
        headers = headers,
        tokenProvider = tokenProvider,
        tokenParams = tokenParams,
        retryOptions = retryOptions
    )

    @JvmOverloads
    fun <A> request(
        options: RequestOptions,
        responseParser: DataParser<A>,
        tokenProvider: TokenProvider? = instanceTokenProvider,
        tokenParams: Any? = instanceTokenParams
    ): Future<Result<A, Error>> = baseClient.request(
        requestDestination = options.destination.toScopedDestination(),
        headers = options.headers,
        method = options.method,
        body = options.body,
        tokenProvider = tokenProvider,
        tokenParams = tokenParams,
        responseParser = responseParser
    )

    @Suppress("unused") // Public API
    @JvmOverloads
    fun <A> upload(
        path: String,
        headers: Headers = emptyHeaders(),
        file: File,
        responseParser: DataParser<A>,
        tokenProvider: TokenProvider? = instanceTokenProvider,
        tokenParams: Any? = instanceTokenParams
    ): Future<Result<A, Error>> = upload(
        requestDestination = Relative(path),
        headers = headers,
        file = file,
        responseParser = responseParser,
        tokenProvider = tokenProvider,
        tokenParams = tokenParams
    )

    @JvmOverloads
    fun <A> upload(
        requestDestination: RequestDestination,
        headers: Headers = emptyHeaders(),
        file: File,
        responseParser: DataParser<A>,
        tokenProvider: TokenProvider? = instanceTokenProvider,
        tokenParams: Any? = instanceTokenParams
    ): Future<Result<A, Error>> = baseClient.upload(
        requestDestination = requestDestination.toScopedDestination(),
        headers = headers,
        file = file,
        responseParser = responseParser,
        tokenProvider = tokenProvider,
        tokenParams = tokenParams
    )

    fun RequestDestination.toScopedDestination(): RequestDestination = when (this) {
        is Absolute -> this
        is Relative -> Relative(scopePathToService(path))
    }

    private fun scopePathToService(relativePath: String): String {
        return "services/$serviceName/$serviceVersion/$id/$relativePath"
    }

}

private data class Locator(val version: String, val cluster: String, val id: String) {
    companion object {
        operator fun invoke(raw: String) : Locator {
            val splitInstanceLocator = raw.split(":")
            splitInstanceLocator.getOrNull(2)
            require(splitInstanceLocator.size == 3) {
                "Expecting locator to be of the form 'v1:us1:1a234-123a-1234-12a3-1234123aa12' but got this instead: '$raw'. Check the dashboard to ensure you have a properly formatted locator."
            }
            val (version, cluster, id) = splitInstanceLocator
            return Locator(version, cluster, id)
        }
    }
}

class SubscriptionListeners<A>(
    val onEnd: (error: EOSEvent?) -> Unit = {},
    val onError: (error: elements.Error) -> Unit = {},
    val onEvent: (event: SubscriptionEvent<A>) -> Unit = {},
    val onOpen: (headers: Headers) -> Unit = {},
    val onRetrying: () -> Unit = {},
    val onSubscribe: () -> Unit = {}
) {

    companion object {

        @JvmStatic
        fun <A> compose(vararg l: SubscriptionListeners<A>) = SubscriptionListeners<A>(
            onEnd = { error -> l.forEach { it.onEnd(error) } },
            onError = { error -> l.forEach { it.onError(error) } },
            onEvent = { event -> l.forEach { it.onEvent(event) } },
            onOpen = { headers -> l.forEach { it.onOpen(headers) } },
            onRetrying = { l.forEach { it.onRetrying() } },
            onSubscribe = { l.forEach { it.onSubscribe() } }
        )

    }

}

