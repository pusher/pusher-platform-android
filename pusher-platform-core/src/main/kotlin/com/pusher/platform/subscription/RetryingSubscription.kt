package com.pusher.platform.subscription

import com.pusher.platform.ErrorResolver
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.retrying.RetryStrategy.DoNotRetry
import com.pusher.platform.retrying.RetryStrategy.Retry
import elements.EOSEvent
import elements.Headers
import elements.Subscription

internal fun <A> createRetryingStrategy(
    errorResolver: ErrorResolver,
    nextSubscribeStrategy: SubscribeStrategy<A>,
    subscriptionID: String,
    logger: Logger
): SubscribeStrategy<A> = { listeners, headers ->
    RetryingSubscription(
        listeners,
        headers,
        subscriptionID,
        logger,
        errorResolver,
        nextSubscribeStrategy
    )
}

private class RetryingSubscription<A>(
        listeners: SubscriptionListeners<A>,
        val headers: Headers,
        val subscriptionID: String,
        val logger: Logger,
        val errorResolver: ErrorResolver,
        val nextSubscribeStrategy: SubscribeStrategy<A>
): Subscription {

    private val onTransition: StateTransition = { newState -> state = newState }

    @Volatile
    private var state: SubscriptionState = OpeningSubscriptionState(listeners, onTransition)

    override fun unsubscribe() {
        state.unsubscribe()
        errorResolver.cancel()
    }

    inner class EndingSubscriptionState : SubscriptionState {
        init {
            logger.verbose("RetryingSubscription $subscriptionID: transitioning to EndingSubscriptionState")
        }

        override fun unsubscribe() {
            logger.verbose("RetryingSubscription $subscriptionID: subscription is already ended; nothing to do for unsubscribe")
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
                    "RetryingSubscription $subscriptionID: received event $event"
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
            logger.verbose("RetryingSubscription $subscriptionID: transitioning to OpeningSubscriptionState")
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
        @Volatile
        var underlyingSubscription: Subscription? = null

        init {
            logger.verbose("RetryingSubscription $subscriptionID: transitioning to RetryingSubscriptionState")
            executeSubscriptionOnce(error)
        }

        override fun unsubscribe() {
            underlyingSubscription?.unsubscribe()
        }

        private fun executeSubscriptionOnce(error: elements.Error) {
            errorResolver.resolveError(error) { resolution ->
                when (resolution) {
                    is DoNotRetry -> onTransition(FailedSubscriptionState(listeners, error))
                    is Retry -> executeNextSubscribeStrategy()
                }
            }
        }

        private fun executeNextSubscribeStrategy() {
            logger.verbose("RetryingSubscription $subscriptionID: trying to re-establish the subscription")

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
                                        "RetryingSubscription $subscriptionID: received event $event"
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
            logger.verbose("RetryingSubscription $subscriptionID: transitioning to OpenSubscriptionState")
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
            logger.verbose("RetryingSubscription $subscriptionID: transitioning to EndedSubscriptionState")
            listeners.onEnd(error)
        }

        override fun unsubscribe() {
            logger.verbose("RetryingSubscription $subscriptionID: Subscription has already ended; nothing to do for unsubscribe")
        }
    }

    inner class FailedSubscriptionState(
            listeners: SubscriptionListeners<A>,
            error: elements.Error
    ): SubscriptionState {
        init {
            logger.verbose("RetryingSubscription $subscriptionID: transitioning to FailedSubscriptionState")
            listeners.onError(error)
        }

        override fun unsubscribe() {
            logger.verbose("RetryingSubscription $subscriptionID: Subscription has already ended; nothing to do for unsubscribe")
        }
    }
}
