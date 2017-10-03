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
        instanceId: String,
        val serviceName: String,
        val serviceVersion: String,
        host: String? = null,
        logger: Logger = AndroidLogger(threshold = LogLevel.DEBUG),
        context: Context
        ) {

    val HOST_BASE = "pusherplatform.io"
    val id: String = instanceId.split(":")[2]
    val cluster: String = instanceId.split(":")[1]
    val platformVersion: String = instanceId.split(":")[0]
    val host: String = host ?: "$cluster.$HOST_BASE"


    val baseClient: BaseClient = BaseClient(host = this.host, logger = logger, context = context)

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



