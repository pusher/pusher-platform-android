package com.pusher.platform.subscription

import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.Result
import elements.*
import java.util.concurrent.Future


internal fun <A> createTokenProvidingStrategy(
    nextSubscribeStrategy: SubscribeStrategy<A>,
    subscriptionID: String,
    logger: Logger,
    tokenProvider: TokenProvider? = null,
    tokenParams: Any? = null
): SubscribeStrategy<A> = when {
    // Token provider might not be provided. If missing, go straight to underlying subscribe strategy
    tokenProvider != null -> { listeners, headers ->
        TokenProvidingSubscription(
            subscriptionID,
            logger,
            listeners,
            headers,
            tokenProvider,
            tokenParams,
            nextSubscribeStrategy
        )
    }
    else -> nextSubscribeStrategy
}

internal class TokenProvidingSubscription<A>(
    private val subscriptionID: String,
    private val logger: Logger,
    private val listeners: SubscriptionListeners<A>,
    headers: Headers,
    private val tokenProvider: TokenProvider,
    private val tokenParams: Any? = null,
    nextSubscribeStrategy: SubscribeStrategy<A>
) : Subscription {
    private var state: TokenProvidingSubscriptionState<A> = ActiveState(subscriptionID, logger, headers, nextSubscribeStrategy)
    private var tokenRequestInProgress: Future<Result<String, Error>>? = null

    init {
        tokenRequestInProgress = subscribe()
    }

    override fun unsubscribe() {
        tokenRequestInProgress?.cancel(true)
        state.unsubscribe()
        state = InactiveState(subscriptionID, logger)
    }

    private fun subscribe(): Future<Result<String, Error>> = tokenProvider.fetchToken(tokenParams).apply {
            get()
                .fold({ error ->
                logger.debug(
                    "TokenProvidingSubscription $subscriptionID: error when fetching token: $error"
                )
                state = InactiveState(subscriptionID, logger)
                listeners.onError(error)
            }, { token ->
                state.subscribe(token, SubscriptionListeners(
                    onEnd = { error: EOSEvent? ->
                        state = InactiveState(subscriptionID, logger)
                        listeners.onEnd(error)
                    },
                    onError = { error ->
                        if (error.tokenExpired()) {
                            tokenProvider.clearToken(token)
                            tokenRequestInProgress = subscribe()
                        } else {
                            state = InactiveState(subscriptionID, logger)
                            listeners.onError(error)
                        }
                    },
                    onEvent = listeners.onEvent,
                    onOpen = listeners.onOpen
                ))
            })
        }
}

internal interface TokenProvidingSubscriptionState<A> {
    fun subscribe(token: String, listeners: SubscriptionListeners<A>)
    fun unsubscribe()
}

internal class ActiveState<A>(
    private val subscriptionID: String,
    private val logger: Logger,
    private val headers: Headers,
    private val nextSubscribeStrategy: SubscribeStrategy<A>
) : TokenProvidingSubscriptionState<A> {
    private var underlyingSubscription: Subscription? = null

    override fun subscribe(token: String, listeners: SubscriptionListeners<A>) {
        this.underlyingSubscription = this.nextSubscribeStrategy(
            SubscriptionListeners(
                onEnd = { error ->
                    this.logger.verbose("TokenProvidingSubscription $subscriptionID: subscription ended")
                    listeners.onEnd(error)
                },
                onError = { error ->
                    this.logger.verbose(
                        "TokenProvidingSubscription $subscriptionID: subscription errored: $error"
                    )
                    listeners.onError(error)
                },
                onEvent = listeners.onEvent,
                onOpen = { headers ->
                    this.logger.verbose("TokenProvidingSubscription $subscriptionID: subscription opened")
                    listeners.onOpen(headers)
                },
                onRetrying = listeners.onRetrying
            ),
            headers.withToken(token)
        )
    }

    override fun unsubscribe() {
        this.underlyingSubscription?.unsubscribe()
    }
}

internal class InactiveState<A>(
        private val subscriptionID: String,
        private val logger: Logger
) : TokenProvidingSubscriptionState<A> {
    init {
        logger.verbose("TokenProvidingSubscription $subscriptionID: transitioning to InactiveState")
    }

    override fun subscribe(token: String, listeners: SubscriptionListeners<A>) {
        logger.verbose(
            "TokenProvidingSubscription $subscriptionID: subscribe called in Inactive state; doing nothing"
        )
    }

    override fun unsubscribe() {
        logger.verbose(
            "TokenProvidingSubscription $subscriptionID: unsubscribe called in Inactive state; doing nothing"
        )
    }
}

private fun Headers.withToken(token: String): Headers =
    this + ("Authorization" to listOf("Bearer $token"))

private fun elements.Error.tokenExpired(): Boolean {
    return this is ErrorResponse && this.statusCode == 401 && this.error == "authentication/expired"
}
