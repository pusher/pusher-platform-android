package com.pusher.platform

import com.pusher.platform.network.OkHttpResponsePromise
import com.pusher.platform.retrying.RetryStrategyOptions
import com.pusher.platform.tokenProvider.TokenProvider
import elements.EOSEvent
import elements.Headers
import elements.Subscription
import elements.SubscriptionEvent
import java.io.File
import java.util.*

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
        headers: Headers = TreeMap(String.CASE_INSENSITIVE_ORDER),
        tokenProvider: TokenProvider? = null,
        tokenParams: Any? = null,
        retryOptions: RetryStrategyOptions = RetryStrategyOptions(),
        initialEventId: String? = null
    ): Subscription {
        return subscribeResuming(
            requestDestination = RequestDestination.Relative(path),
            listeners = listeners,
            headers = headers,
            tokenProvider = tokenProvider,
            tokenParams = tokenParams,
            retryOptions = retryOptions,
            initialEventId = initialEventId
        )
    }

    fun subscribeResuming(
        requestDestination: RequestDestination,
        listeners: SubscriptionListeners,
        headers: Headers = TreeMap(String.CASE_INSENSITIVE_ORDER),
        tokenProvider: TokenProvider? = null,
        tokenParams: Any? = null,
        retryOptions: RetryStrategyOptions = RetryStrategyOptions(),
        initialEventId: String? = null
    ): Subscription {
        val destination = scopeDestinationIfAppropriate(requestDestination)

        return this.baseClient.subscribeResuming(
            requestDestination = destination,
            listeners = listeners,
            headers = headers,
            tokenProvider = tokenProvider,
            tokenParams = tokenParams,
            retryOptions = retryOptions,
            initialEventId = initialEventId
        )
    }

    fun subscribeNonResuming(
        path: String,
        listeners: SubscriptionListeners,
        headers: Headers = TreeMap(String.CASE_INSENSITIVE_ORDER),
        tokenProvider: TokenProvider? = null,
        tokenParams: Any? = null,
        retryOptions: RetryStrategyOptions = RetryStrategyOptions()
    ): Subscription {
        return subscribeNonResuming(
            requestDestination = RequestDestination.Relative(path),
            listeners = listeners,
            headers = headers,
            tokenProvider = tokenProvider,
            tokenParams = tokenParams,
            retryOptions = retryOptions
        )
    }

    fun subscribeNonResuming(
        requestDestination: RequestDestination,
        listeners: SubscriptionListeners,
        headers: Headers = TreeMap(String.CASE_INSENSITIVE_ORDER),
        tokenProvider: TokenProvider? = null,
        tokenParams: Any? = null,
        retryOptions: RetryStrategyOptions = RetryStrategyOptions()
    ): Subscription = this.baseClient.subscribeNonResuming(
        requestDestination = scopeDestinationIfAppropriate(requestDestination),
        listeners = listeners,
        headers = headers,
        tokenProvider = tokenProvider,
        tokenParams = tokenParams,
        retryOptions = retryOptions
    )

    fun request(
        options: RequestOptions,
        tokenProvider: TokenProvider? = null,
        tokenParams: Any? = null
    ): OkHttpResponsePromise = this.baseClient.request(
        requestDestination = scopeDestinationIfAppropriate(options.destination),
        headers = options.headers,
        method = options.method,
        body = options.body,
        tokenProvider = tokenProvider,
        tokenParams = tokenParams
    )

    fun upload(
        path: String,
        headers: elements.Headers = TreeMap(),
        file: File,
        tokenProvider: TokenProvider? = null,
        tokenParams: Any? = null
    ): OkHttpResponsePromise = upload(
        requestDestination = RequestDestination.Relative(path),
        headers = headers,
        file = file,
        tokenProvider = tokenProvider,
        tokenParams = tokenParams
    )

    fun upload(
        requestDestination: RequestDestination,
        headers: elements.Headers = TreeMap(),
        file: File,
        tokenProvider: TokenProvider? = null,
        tokenParams: Any? = null
    ): OkHttpResponsePromise {
        val destination = scopeDestinationIfAppropriate(requestDestination)

        return this.baseClient.upload(destination, headers, file, tokenProvider, tokenParams)
    }

    private fun scopeDestinationIfAppropriate(destination: RequestDestination): RequestDestination {
        return when (destination) {
            is RequestDestination.Absolute -> {
                destination
            }
            is RequestDestination.Relative -> {
                RequestDestination.Relative(scopePathToService(destination.path))
            }
        }
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
                "Expecting locator to be of the form 'v1:us1:1a234-123a-1234-12a3-1234123aa12' but got this instead: $this'. Check the dashboard to ensure you have a properly formatted locator."
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

