package com.pusher.platform

import com.pusher.platform.logger.Logger
import com.pusher.platform.retrying.RetryStrategyOptions
import com.pusher.platform.subscription.BaseSubscription
import com.pusher.platform.subscription.SubscribeStrategy
import com.pusher.platform.tokenProvider.TokenProvider
import elements.Headers
import elements.Subscription

class BaseClient(
        var host: String,
        val logger: Logger,
        encrypted: Boolean = true) {

        val baseUrl: String
    init{

        val prefix = if(encrypted) "https" else "http"
        baseUrl = "$prefix://$host"
    }

    fun subscribeResuming(
            path: String,
            listeners: SubscriptionListeners,
            headers: Headers,
            tokenProvider: TokenProvider? = null,
            retryOptions: RetryStrategyOptions? = null
    ): Subscription {

        val subscribeStrategy: SubscribeStrategy = createResumingStrategy(
                retryOptions = retryOptions,
                initialEventId = "",
                logger = logger,
                nextSubscribeStrategy = createTokenProvidingStrategy(
                        tokenProvider = tokenProvider,
                        logger = logger,
                        nextSubscribeStrategy = createHTTP2TransportStrategy(path = path))
        )


        return subscribeStrategy(listeners, headers)
    }

    fun justFuckingSubscribe(
            path: String,
            listeners: SubscriptionListeners,
            headers: Headers
    ) : Subscription {



        return BaseSubscription(path = absolutePath(path), headers = headers, onOpen = listeners.onOpen, onError =  listeners.onError, onEvent = listeners.onEvent, onEnd = listeners.onEnd)
    }
    fun absolutePath(path: String): String  = "$baseUrl/$path"

}




fun createResumingStrategy(
        retryOptions: RetryStrategyOptions?,
        initialEventId: String? = null,
        nextSubscribeStrategy: SubscribeStrategy,
        logger: Logger): SubscribeStrategy {

    return nextSubscribeStrategy
}

fun createTokenProvidingStrategy(tokenProvider: TokenProvider?, logger: Logger, nextSubscribeStrategy: SubscribeStrategy): SubscribeStrategy {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}

fun createHTTP2TransportStrategy(path: String): SubscribeStrategy {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}