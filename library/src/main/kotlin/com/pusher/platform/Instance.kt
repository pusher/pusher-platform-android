package com.pusher.platform

import android.content.Context
import com.pusher.platform.logger.AndroidLogger
import com.pusher.platform.logger.LogLevel
import com.pusher.platform.logger.Logger
import com.pusher.platform.retrying.RetryStrategyOptions
import com.pusher.platform.tokenProvider.TokenProvider
import elements.EOSEvent
import elements.Error
import elements.Headers
import elements.Subscription
import elements.SubscriptionEvent
import okhttp3.Response
import java.io.File
import java.util.*


class Instance(
        locator: String,
        val serviceName: String,
        val serviceVersion: String,
        baseClient: BaseClient? = null,
        host: String? = null,
        logger: Logger = AndroidLogger(threshold = LogLevel.DEBUG),
        context: Context
        ) {

    val id: String
    val cluster: String
    val platformVersion: String
    val serviceHost: String
    val baseClient: BaseClient

    companion object {
        const val HOST_BASE = "pusherplatform.io"
    }

    init {
        val splitInstanceLocator = locator.split(":")
        if(splitInstanceLocator.size != 3) throw IllegalArgumentException("Expecting locator to be of the form 'v1:us1:1a234-123a-1234-12a3-1234123aa12' but got this instead: $locator'. Check the dashboard to ensure you have a properly formatted locator.")

        id = splitInstanceLocator[2]
        cluster = splitInstanceLocator[1]
        platformVersion = splitInstanceLocator[0]

        serviceHost = host ?: "$cluster.$HOST_BASE"
        this.baseClient = baseClient ?: BaseClient(host = serviceHost, logger = logger, context = context)
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
    ): Subscription {
        val destination = scopeDestinationIfAppropriate(requestDestination)

        return this.baseClient.subscribeNonResuming(
                requestDestination = destination,
                listeners = listeners,
                headers = headers,
                tokenProvider = tokenProvider,
                tokenParams = tokenParams,
                retryOptions = retryOptions
        )
    }

    fun request(
            options: RequestOptions,
            tokenProvider: TokenProvider? = null,
            tokenParams: Any? = null,
            onSuccess: (Response) -> Unit,
            onFailure: (elements.Error) -> Unit
    ): Cancelable {
        val destination = scopeDestinationIfAppropriate(options.destination)

        return this.baseClient.request(
                requestDestination = destination,
                headers = options.headers,
                method = options.method,
                body = options.body,
                tokenProvider = tokenProvider,
                tokenParams = tokenParams,
                onSuccess = onSuccess,
                onFailure = onFailure
        )
    }

    fun upload(path: String,
               headers: elements.Headers = TreeMap(),
               file: File,
               tokenProvider: TokenProvider? = null,
               tokenParams: Any? = null,
               onSuccess: (Response) -> Unit,
               onFailure: (Error) -> Unit
    ): Cancelable? {
        return upload(
                requestDestination = RequestDestination.Relative(path),
                headers = headers,
                file = file,
                tokenProvider = tokenProvider,
                tokenParams = tokenParams,
                onSuccess = onSuccess,
                onFailure = onFailure
        )
    }

    fun upload(requestDestination: RequestDestination,
               headers: elements.Headers = TreeMap(),
               file: File,
               tokenProvider: TokenProvider? = null,
               tokenParams: Any? = null,
               onSuccess: (Response) -> Unit,
               onFailure: (Error) -> Unit): Cancelable? {
        val destination = scopeDestinationIfAppropriate(requestDestination)

        return this.baseClient.upload(destination, headers, file, tokenProvider, tokenParams, onSuccess, onFailure)
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
        return "services/${this.serviceName}/${this.serviceVersion}/${this.id}/${relativePath}"
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
