package com.pusher.platform

import com.pusher.platform.logger.AndroidLogger
import com.pusher.platform.logger.LogLevel
import com.pusher.platform.logger.Logger
import com.pusher.platform.retrying.RetryStrategyOptions
import com.pusher.platform.tokenProvider.TokenProvider
import elements.EOSEvent
import elements.Headers
import elements.Subscription
import elements.SubscriptionEvent
import java.util.*


class Instance(
        instanceId: String,
        val serviceName: String,
        val serviceVersion: String,
        host: String? = null,
        logger: Logger = AndroidLogger(threshold = LogLevel.DEBUG)
        ) {

    val HOST_BASE = "pusherplatform.io"
    val id: String = instanceId.split(":")[2]
    val cluster: String = instanceId.split(":")[1]
    val platformVersion: String = instanceId.split(":")[0]
    val host: String = host ?: "$cluster.$HOST_BASE"


    val baseClient: BaseClient = BaseClient(host = this.host, logger = logger)

    fun justFuckingSubscribe(path: String, listeners: SubscriptionListeners, headers: Headers? = TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER)): Subscription {

        val subscription = baseClient.justFuckingSubscribe(
                path = absPath(path),
                listeners = listeners,
                headers = headers
        )

        return subscription
    }


    fun subscribeResuming(
            path: String,
            listeners: SubscriptionListeners,
            headers: Headers = TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER),
            tokenProvider: TokenProvider? = null,
            retryOptions: RetryStrategyOptions = RetryStrategyOptions()
            ): Subscription {

        return baseClient.subscribeResuming(
                path = absPath(path),
                listeners = listeners,
                headers = headers,
                tokenProvider = tokenProvider,
                retryOptions = retryOptions
        )
    }

    fun subscribeNonResuming(): Subscription {
        //TODO("Not yet implemented")
        throw NotImplementedError("Not yet implemented")
    }

    fun request(options: RequestOptions): CancelableRequest {
        //TODO("Not yet implemented")
        throw NotImplementedError("Not yet implemented")
    }

    fun absPath(relativePath: String): String {
        return "services/${this.serviceName}/${this.serviceVersion}/${this.id}/${relativePath}"
    }

}

class SubscriptionListeners(
        val onOpen: (headers: Headers) -> Unit,
        val onSubscribe: () -> Unit,
        val onRetrying: () -> Unit,
        val onEvent: (event: SubscriptionEvent) -> Unit,
        val onError: (error: elements.Error) -> Unit,
        val onEnd: (error: EOSEvent?) -> Unit)



