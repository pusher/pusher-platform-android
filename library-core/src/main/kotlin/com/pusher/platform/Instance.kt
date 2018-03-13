package com.pusher.platform

import com.pusher.platform.logger.Logger
import com.pusher.platform.network.ConnectivityHelper
import com.pusher.platform.network.OkHttpResponsePromise
import com.pusher.platform.retrying.RetryStrategyOptions
import com.pusher.platform.tokenProvider.TokenProvider
import elements.*
import java.io.File
import java.util.*


open class Instance(
    locator: String,
    val serviceName: String,
    val serviceVersion: String,
    logger: Logger,
    scheduler: Scheduler,
    mainThreadScheduler: MainThreadScheduler,
    mediatypeResolver: MediaTypeResolver,
    connectivityHelper: ConnectivityHelper,
    baseClient: BaseClient? = null,
    host: String? = null
) {

    private val id: String
    private val serviceHost: String
    private val baseClient: BaseClient

    companion object {
        const val HOST_BASE = "pusherplatform.io"
    }

    init {
        val splitInstanceLocator = locator.split(":")
        require(splitInstanceLocator.size == 3) {
            "Expecting locator to be of the form 'v1:us1:1a234-123a-1234-12a3-1234123aa12' but got this instead: $locator'. Check the dashboard to ensure you have a properly formatted locator."
        }

        val (_, cluster, id) = splitInstanceLocator

        this.id = id

        serviceHost = host ?: "$cluster.$HOST_BASE"
        this.baseClient = baseClient ?: BaseClient(
            host = serviceHost,
            logger = logger,
            scheduler = scheduler,
            mainScheduler = mainThreadScheduler,
            connectivityHelper = connectivityHelper,
            mediaTypeResolver = mediatypeResolver
        )
    }

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

class SubscriptionListeners(
    val onEnd: (error: EOSEvent?) -> Unit = {},
    val onError: (error: elements.Error) -> Unit = {},
    val onEvent: (event: SubscriptionEvent) -> Unit = {},
    val onOpen: (headers: Headers) -> Unit = {},
    val onRetrying: () -> Unit = {},
    val onSubscribe: () -> Unit = {}
)
