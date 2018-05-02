package com.pusher.platform.subscription

import com.pusher.platform.ErrorResolver
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.Futures
import com.pusher.platform.network.waitOr
import com.pusher.platform.retrying.RetryStrategy
import elements.EOSEvent
import elements.Headers
import elements.Subscription

fun <A> createRetryingStrategy(
    errorResolver: ErrorResolver,
    nextSubscribeStrategy: SubscribeStrategy<A>,
    logger: Logger
): SubscribeStrategy<A> = { listeners, headers ->
    RetryingSubscription(
        listeners,
        headers,
        logger,
        errorResolver,
        nextSubscribeStrategy
    )
}

private class RetryingSubscription<A>(
        listeners: SubscriptionListeners<A>,
        val headers: Headers,
        val logger: Logger,
        val errorResolver: ErrorResolver,
        val nextSubscribeStrategy: SubscribeStrategy<A>
): Subscription {
    var state: SubscriptionState

    val onTransition: StateTransition = { newState -> state = newState }

    init {
        state = OpeningSubscriptionState(listeners, onTransition)
    }

    override fun unsubscribe() {
        this.state.unsubscribe()
    }

    inner class EndingSubscriptionState : SubscriptionState {
        init {
            logger.verbose("${RetryingSubscription@this}: transitioning to EndingSubscriptionState")
        }

        override fun unsubscribe() {
            throw Error("Subscription is already ending")
        }
    }

    inner class OpeningSubscriptionState(
            listeners: SubscriptionListeners<A>,
            onTransition: StateTransition
    ): SubscriptionState {

        private val underlyingListeners = SubscriptionListeners<A>(
            onOpen = { headers ->
                onTransition(
                    OpenSubscriptionState(
                        listeners,
                        headers,
                        this,
                        onTransition
                    )
                )
            },
            onSubscribe = listeners.onSubscribe,
            onEvent = { event ->
                listeners.onEvent(event)
                logger.verbose(
                    "${RetryingSubscription@this}received event $event"
                )
            },
            onRetrying = listeners.onRetrying,
            onError = { error ->
                onTransition(
                    RetryingSubscriptionState(listeners, error, onTransition)
                )
            },
            onEnd = { error: EOSEvent? ->
                onTransition(EndedSubscriptionState(listeners, error))
            }
        )

        private val underlyingSubscription: Subscription = nextSubscribeStrategy(underlyingListeners, headers)

        init {
            logger.verbose("${RetryingSubscription@this}: transitioning to OpeningSubscriptionState")
        }

        override fun unsubscribe() {
            onTransition(EndingSubscriptionState())
            underlyingSubscription.unsubscribe()
        }
    }

    inner class RetryingSubscriptionState(
            val listeners: SubscriptionListeners<A>,
            error: elements.Error,
            val onTransition: StateTransition
    ): SubscriptionState {
        var underlyingSubscription: Subscription? = null

        init {
            logger.verbose("${RetryingSubscription@this}: transitioning to RetryingSubscriptionState")
            executeSubscriptionOnce(error)
        }

        override fun unsubscribe() {
            underlyingSubscription?.unsubscribe()
        }

        private fun executeSubscriptionOnce(
            error: elements.Error
        ) = Futures.schedule {
            val resolution = errorResolver.resolveError(error).waitOr { RetryStrategy.DoNotRetry }
            when(resolution) {
                is RetryStrategy.DoNotRetry -> onTransition(FailedSubscriptionState(listeners, error))
                is RetryStrategy.Retry -> executeNextSubscribeStrategy()
            }
        }

        private fun executeNextSubscribeStrategy() {
            logger.verbose("${RetryingSubscription@this}: trying to re-establish the subscription")

            underlyingSubscription = nextSubscribeStrategy(
                    SubscriptionListeners(
                            onOpen = { headers ->
                                onTransition(
                                        OpenSubscriptionState(
                                                listeners,
                                                headers,
                                                underlyingSubscription!!,
                                                onTransition
                                        )
                                )
                            },
                            onRetrying = listeners.onRetrying,
                            onError = {
                                error -> executeSubscriptionOnce(error)
                            },
                            onEvent = { event ->
                                listeners.onEvent(event)
                                logger.verbose(
                                        "${RetryingSubscription@this}received event $event"
                                )
                            },
                            onSubscribe = listeners.onSubscribe,
                            onEnd = { error ->
                                onTransition(EndedSubscriptionState(listeners, error))
                            }
                    ),
                    headers
            )
        }
    }

    inner class OpenSubscriptionState(
            listeners: SubscriptionListeners<A>,
            headers: Headers,
            val underlyingSubscription: Subscription,
            val onTransition: StateTransition
    ): SubscriptionState {
        init {
            logger.verbose("${RetryingSubscription@this}: transitioning to OpenSubscriptionState")
            listeners.onOpen(headers)
        }

        override fun unsubscribe() {
            onTransition(EndingSubscriptionState())
            underlyingSubscription.unsubscribe()
        }
    }

    inner class EndedSubscriptionState(
            listeners: SubscriptionListeners<A>,
            error: EOSEvent?
    ): SubscriptionState {
        init {
            logger.verbose("${RetryingSubscription@this}: transitioning to EndedSubscriptionState")
            listeners.onEnd(error)
        }

        override fun unsubscribe() {
            throw Error("Subscription has already ended")
        }
    }

    inner class FailedSubscriptionState(
            listeners: SubscriptionListeners<A>,
            error: elements.Error
    ): SubscriptionState {
        init {
            logger.verbose("${RetryingSubscription@this}: transitioning to FailedSubscriptionState")
            listeners.onError(error)
        }

        override fun unsubscribe() {
            throw Error("Subscription has already ended")
        }
    }
}
