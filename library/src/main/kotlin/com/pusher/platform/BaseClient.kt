package com.pusher.platform

import android.content.Context
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.ConnectivityHelper
import com.pusher.platform.retrying.RetryStrategyOptions
import com.pusher.platform.subscription.BaseSubscription
import com.pusher.platform.subscription.SubscribeStrategy
import com.pusher.platform.subscription.createResumingStrategy
import com.pusher.platform.tokenProvider.TokenProvider
import elements.Headers
import elements.Subscription

class BaseClient(
        var host: String,
        val logger: Logger,
        encrypted: Boolean = true,
        val context: Context) {

        val prefix = if(encrypted) "https" else "http"
        val baseUrl = "$prefix://$host"

    fun subscribeResuming(
            path: String,
            listeners: SubscriptionListeners,
            headers: Headers,
            tokenProvider: TokenProvider? = null,
            retryOptions: RetryStrategyOptions = RetryStrategyOptions()
    ): Subscription {

        val subscribeStrategy: SubscribeStrategy = createResumingStrategy(
                initialEventId = "",
                logger = logger,
                nextSubscribeStrategy = createTokenProvidingStrategy(
                        tokenProvider = tokenProvider,
                        logger = logger,
                        nextSubscribeStrategy = createBaseSubscription(path = path)),
                errorResolver = ErrorResolver(ConnectivityHelper(context), retryOptions)
        )

        return subscribeStrategy(listeners, headers)
    }

    fun justFuckingSubscribe(
            path: String,
            listeners: SubscriptionListeners,
            headers: Headers
    ) : Subscription {

//        🚀
        return BaseSubscription(path = absolutePath(path), headers = headers, onOpen = listeners.onOpen, onError =  listeners.onError, onEvent = listeners.onEvent, onEnd = listeners.onEnd)
    }
    fun absolutePath(path: String): String  = "$baseUrl/$path"

}


fun createTokenProvidingStrategy(tokenProvider: TokenProvider?, logger: Logger, nextSubscribeStrategy: SubscribeStrategy): SubscribeStrategy {
    //TODO
    return { listeners, headers ->
        nextSubscribeStrategy(
                listeners,
                headers
        )
    }
}

fun createBaseSubscription(path: String): SubscribeStrategy {
    return { listeners, headers ->
        BaseSubscription(
                path = path,
                headers = headers,
                onOpen = listeners.onOpen,
                onError = listeners.onError,
                onEvent = listeners.onEvent,
                onEnd = listeners.onEnd
        )
    }
}