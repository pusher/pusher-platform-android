package com.pusher.platform

import com.pusher.platform.RequestDestination.Absolute
import com.pusher.platform.RequestDestination.Relative
import com.pusher.platform.network.typeToken
import com.pusher.platform.retrying.RetryStrategyOptions
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.Result
import elements.*
import java.io.File
import java.lang.reflect.Type
import java.util.*
import java.util.concurrent.Future

private const val DEFAULT_HOST_BASE = "pusherplatform.io"

data class Instance constructor(
    val id: String,
    val baseClient: BaseClient,
    val serviceName: String,
    val serviceVersion: String
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

    fun subscribeResuming(
        path: String,
        listeners: SubscriptionListeners,
        headers: Headers = emptyHeaders(),
        tokenProvider: TokenProvider? = null,
        tokenParams: Any? = null,
        retryOptions: RetryStrategyOptions = RetryStrategyOptions(),
        initialEventId: String? = null
    ): Subscription = subscribeResuming(
        requestDestination = Relative(path),
        listeners = listeners,
        headers = headers,
        tokenProvider = tokenProvider,
        tokenParams = tokenParams,
        retryOptions = retryOptions,
        initialEventId = initialEventId
    )

    fun subscribeResuming(
        requestDestination: RequestDestination,
        listeners: SubscriptionListeners,
        headers: Headers = TreeMap(String.CASE_INSENSITIVE_ORDER),
        tokenProvider: TokenProvider? = null,
        tokenParams: Any? = null,
        retryOptions: RetryStrategyOptions = RetryStrategyOptions(),
        initialEventId: String? = null
    ): Subscription = baseClient.subscribeResuming(
        destination = requestDestination.toScopedDestination(),
        listeners = listeners,
        headers = headers,
        tokenProvider = tokenProvider,
        tokenParams = tokenParams,
        retryOptions = retryOptions,
        initialEventId = initialEventId
    )

    fun subscribeNonResuming(
        path: String,
        listeners: SubscriptionListeners,
        headers: Headers = emptyHeaders(),
        tokenProvider: TokenProvider? = null,
        tokenParams: Any? = null,
        retryOptions: RetryStrategyOptions = RetryStrategyOptions()
    ): Subscription = subscribeNonResuming(
        requestDestination = Relative(path),
        listeners = listeners,
        headers = headers,
        tokenProvider = tokenProvider,
        tokenParams = tokenParams,
        retryOptions = retryOptions
    )

    fun subscribeNonResuming(
        requestDestination: RequestDestination,
        listeners: SubscriptionListeners,
        headers: Headers = emptyHeaders(),
        tokenProvider: TokenProvider? = null,
        tokenParams: Any? = null,
        retryOptions: RetryStrategyOptions = RetryStrategyOptions()
    ): Subscription = baseClient.subscribeNonResuming(
        destination = requestDestination.toScopedDestination(),
        listeners = listeners,
        headers = headers,
        tokenProvider = tokenProvider,
        tokenParams = tokenParams,
        retryOptions = retryOptions
    )

    @JvmOverloads
    inline fun <reified A> request(
        options: RequestOptions,
        type: Type = typeToken<A>(),
        tokenProvider: TokenProvider? = null,
        tokenParams: Any? = null
    ): Future<Result<A, Error>> = baseClient.request(
        requestDestination = options.destination.toScopedDestination(),
        headers = options.headers,
        method = options.method,
        body = options.body,
        tokenProvider = tokenProvider,
        tokenParams = tokenParams,
        type = type
    )

    @JvmOverloads
    inline fun <reified A> upload(
        path: String,
        headers: Headers = emptyHeaders(),
        file: File,
        type: Class<A> = A::class.java,
        tokenProvider: TokenProvider? = null,
        tokenParams: Any? = null
    ): Future<Result<A, Error>> = upload(
        requestDestination = Relative(path),
        headers = headers,
        file = file,
        type = type,
        tokenProvider = tokenProvider,
        tokenParams = tokenParams
    )

    @JvmOverloads
    inline fun <reified A> upload(
        requestDestination: RequestDestination,
        headers: Headers = emptyHeaders(),
        file: File,
        type: Class<A> = A::class.java,
        tokenProvider: TokenProvider? = null,
        tokenParams: Any? = null
    ): Future<Result<A, Error>> = baseClient.upload(
        requestDestination = requestDestination.toScopedDestination(),
        headers = headers,
        file = file,
        type = type,
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

class SubscriptionListeners(
    val onEnd: (error: EOSEvent?) -> Unit = {},
    val onError: (error: elements.Error) -> Unit = {},
    val onEvent: (event: SubscriptionEvent) -> Unit = {},
    val onOpen: (headers: Headers) -> Unit = {},
    val onRetrying: () -> Unit = {},
    val onSubscribe: () -> Unit = {}
) {

    companion object {

        @JvmStatic
        fun compose(vararg l: SubscriptionListeners) = SubscriptionListeners(
            onEnd = { error -> l.forEach { it.onEnd(error) } },
            onError = { error -> l.forEach { it.onError(error) } },
            onEvent = { event -> l.forEach { it.onEvent(event) } },
            onOpen = { headers -> l.forEach { it.onOpen(headers) } },
            onRetrying = { l.forEach { it.onRetrying() } },
            onSubscribe = { l.forEach { it.onSubscribe() } }
        )

    }

}

