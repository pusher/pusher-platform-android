package com.pusher.platform.subscription

import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.Result
import elements.*
import java.util.concurrent.Future


fun <A> createTokenProvidingStrategy(
    nextSubscribeStrategy: SubscribeStrategy<A>,
    logger: Logger,
    tokenProvider: TokenProvider? = null,
    tokenParams: Any? = null
): SubscribeStrategy<A> = when {
    // Token provider might not be provided. If missing, go straight to underlying subscribe strategy
    tokenProvider != null -> { listeners, headers ->
        TokenProvidingSubscription(
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
    val logger: Logger,
    val listeners: SubscriptionListeners<A>,
    val headers: Headers,
    val tokenProvider: TokenProvider,
    val tokenParams: Any? = null,
    val nextSubscribeStrategy: SubscribeStrategy<A>
) : Subscription {
    private var state: TokenProvidingSubscriptionState<A>
    private var tokenRequestInProgress: Future<Result<String, Error>>? = null

    init {
        state = ActiveState(logger, headers, nextSubscribeStrategy)
        this.subscribe()
    }

    override fun unsubscribe() {
        tokenRequestInProgress?.cancel(true)
        state.unsubscribe()
        state = InactiveState(logger)
    }

    private fun subscribe() {
        tokenRequestInProgress = tokenProvider.fetchToken(tokenParams).apply {
            get()
                .fold({ error ->
                logger.debug(
                    "TokenProvidingSubscription: error when fetching token: $error"
                )
                state = InactiveState(logger)
                listeners.onError(error)
            }, { token ->
                state.subscribe(token, SubscriptionListeners(
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
                ))
            })
        }
    }
}

interface TokenProvidingSubscriptionState<A> {
    fun subscribe(token: String, listeners: SubscriptionListeners<A>)
    fun unsubscribe()
}

class ActiveState<A>(
    val logger: Logger,
    val headers: Headers,
    private val nextSubscribeStrategy: SubscribeStrategy<A>
) : TokenProvidingSubscriptionState<A> {
    private var underlyingSubscription: Subscription? = null

    override fun subscribe(token: String, listeners: SubscriptionListeners<A>) {

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
        this.underlyingSubscription?.unsubscribe()
    }
}

class InactiveState<A>(val logger: Logger) : TokenProvidingSubscriptionState<A> {
    init {
        logger.verbose("TokenProvidingSubscription: transitioning to InactiveState")
    }

    override fun subscribe(token: String, listeners: SubscriptionListeners<A>) {
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
