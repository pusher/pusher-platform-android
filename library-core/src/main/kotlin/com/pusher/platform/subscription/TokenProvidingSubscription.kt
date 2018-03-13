package com.pusher.platform.subscription

import com.pusher.platform.Cancelable
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.tokenProvider.TokenProvider
import elements.*


fun createTokenProvidingStrategy(
        nextSubscribeStrategy: SubscribeStrategy,
        logger: Logger,
        tokenProvider: TokenProvider? = null,
        tokenParams: Any? =  null
): SubscribeStrategy {
    // Token provider might not be provided. If missing, go straight to underlying subscribe strategy
    if (tokenProvider != null) {
        return { listeners, headers ->
            TokenProvidingSubscription(
                    logger,
                    listeners,
                    headers,
                    tokenProvider,
                    tokenParams,
                    nextSubscribeStrategy
            )
        }
    }
    return nextSubscribeStrategy
}

class TokenProvidingSubscription(
        val logger: Logger,
        val listeners: SubscriptionListeners,
        val headers: Headers,
        val tokenProvider: TokenProvider,
        val tokenParams: Any? = null,
        val nextSubscribeStrategy: SubscribeStrategy
): Subscription {
    var state: TokenProvidingSubscriptionState
    lateinit var tokenRequestInProgress: Cancelable

    init {
        state = ActiveState(logger, headers, nextSubscribeStrategy)
        this.subscribe()
    }

    override fun unsubscribe(){
        tokenRequestInProgress.cancel()
        state.unsubscribe()
        state = InactiveState(logger)
    }

    private fun subscribe() {
        tokenRequestInProgress = tokenProvider.fetchToken(
                tokenParams = tokenParams,
                onSuccess = { token ->
                    logger.verbose("${TokenProvidingSubscription@this}: token fetched: $token")
                    state.subscribe(
                            token,
                            SubscriptionListeners(
                                    onEnd = { error: EOSEvent? ->
                                        state = InactiveState(logger)
                                        listeners.onEnd(error)
                                    },
                                    onError = { error ->
                                        if (error.tokenExpired()) {
                                            tokenProvider.clearToken(token)
                                            subscribe()
                                        } else {
                                            state = InactiveState(logger)
                                            listeners.onError(error)
                                        }
                                    },
                                    onEvent = listeners.onEvent,
                                    onOpen = listeners.onOpen
                            )
                    )
                },
                onFailure = { error ->
                    logger.debug(
                            "TokenProvidingSubscription: error when fetching token: $error"
                    )
                    state = InactiveState(logger)
                    listeners.onError(error)
                }
        )
    }
}

interface TokenProvidingSubscriptionState {
    fun subscribe(token: String, listeners: SubscriptionListeners)
    fun unsubscribe()
}

class ActiveState(
        val logger: Logger,
        val headers: Headers,
        val nextSubscribeStrategy: SubscribeStrategy
): TokenProvidingSubscriptionState {
    lateinit var underlyingSubscription: Subscription

    override fun subscribe(token: String, listeners: SubscriptionListeners) {

        this.underlyingSubscription = this.nextSubscribeStrategy(
                SubscriptionListeners(
                    onEnd = { error ->
                        this.logger.verbose("TokenProvidingSubscription: subscription ended")
                        listeners.onEnd(error)
                    },
                    onError = { error ->
                        this.logger.verbose(
                            "TokenProvidingSubscription: subscription errored: $error"
                        )
                        listeners.onError(error)
                    },
                    onEvent = listeners.onEvent,
                    onOpen = { headers ->
                        this.logger.verbose("TokenProvidingSubscription: subscription opened")
                        listeners.onOpen(headers)
                    },
                    onRetrying = listeners.onRetrying
                ),
                headers.withToken(token)
        )
    }

    override fun unsubscribe() {
        this.underlyingSubscription.unsubscribe()
    }
}

class InactiveState(val logger: Logger): TokenProvidingSubscriptionState {
    init {
        logger.verbose("TokenProvidingSubscription: transitioning to InactiveState")
    }

    override fun subscribe(token: String, listeners: SubscriptionListeners) {
        logger.verbose(
                "TokenProvidingSubscription: subscribe called in Inactive state; doing nothing"
        )
    }

    override fun unsubscribe() {
        logger.verbose(
                "TokenProvidingSubscription: unsubscribe called in Inactive state; doing nothing"
        )
    }
}

private fun Headers.withToken(token: String): Headers =
    this + ("Authorization" to listOf("Bearer $token"))

private fun elements.Error.tokenExpired(): Boolean {
    return this is ErrorResponse && this.statusCode == 401 && this.error == "authentication/expired"
}
