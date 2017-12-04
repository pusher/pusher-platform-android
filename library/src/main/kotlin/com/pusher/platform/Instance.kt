package com.pusher.platform

import android.content.Context
import com.pusher.platform.logger.AndroidLogger
import com.pusher.platform.logger.LogLevel
import com.pusher.platform.logger.Logger
import com.pusher.platform.retrying.RetryStrategyOptions
import com.pusher.platform.tokenProvider.TokenProvider
import elements.EOSEvent
import elements.Headers
import elements.Subscription
import elements.SubscriptionEvent
import okhttp3.Response
import java.util.*


class Instance(
        locator: String,
        val serviceName: String,
        val serviceVersion: String,
        host: String? = null,
        logger: Logger = AndroidLogger(threshold = LogLevel.DEBUG),
        context: Context
        ) {

    val HOST_BASE = "pusherplatform.io"

    val id: String
    val cluster: String
    val platformVersion: String
    val serviceHost: String
    val baseClient: BaseClient

    init {
        val splitInstanceLocator = locator.split(":")
        if(splitInstanceLocator.size != 3) throw IllegalArgumentException("Expecting locator to be of the form 'v1:us1:1a234-123a-1234-12a3-1234123aa12' but got this instead: $locator'. Check the dashboard to ensure you have a properly formatted locator.")

        id = splitInstanceLocator[2]
        cluster = splitInstanceLocator[1]
        platformVersion = splitInstanceLocator[0]

        serviceHost = host ?: "$cluster.$HOST_BASE"
        baseClient = BaseClient(host = serviceHost, logger = logger, context = context)
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

        return baseClient.subscribeResuming(
                path = absPath(path),
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

        return baseClient.subscribeNonResuming(
                path = absPath(path),
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
            onFailure: (elements.Error) -> Unit ): Cancelable =
         baseClient.request(
                 path = absPath(options.path),
                 headers = options.headers,
                 method = options.method,
                 body = options.body,
                 tokenProvider = tokenProvider,
                 tokenParams = tokenParams,
                 onSuccess = onSuccess,
                 onFailure = onFailure
         )


    private fun absPath(relativePath: String): String {
        return "services/${this.serviceName}/${this.serviceVersion}/${this.id}/${relativePath}"
    }

}

class SubscriptionListeners(
        val onOpen: (headers: Headers) -> Unit = {},
        val onSubscribe: () -> Unit = {},
        val onRetrying: () -> Unit = {},
        val onEvent: (event: SubscriptionEvent) -> Unit = {},
        val onError: (error: elements.Error) -> Unit = {},
        val onEnd: (error: EOSEvent?) -> Unit = {}
)



