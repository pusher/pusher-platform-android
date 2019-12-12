package com.pusher.platform.subscription

import com.pusher.platform.ErrorResolver
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.retrying.RetryStrategy.DoNotRetry
import com.pusher.platform.retrying.RetryStrategy.Retry
import elements.EOSEvent
import elements.Headers
import elements.Subscription

internal fun <A> createResumingStrategy(
    errorResolver: ErrorResolver,
    nextSubscribeStrategy: SubscribeStrategy<A>,
    subscriptionID: String,
    logger: Logger,
    initialEventId: String? = null
): SubscribeStrategy<A> = { listeners, headers ->
    ResumingSubscription(
        listeners,
        headers,
        logger,
        errorResolver,
        nextSubscribeStrategy,
        subscriptionID,
        initialEventId
    )
}

private class ResumingSubscription<A>(
    listeners: SubscriptionListeners<A>,
    val headers: Headers,
    val logger: Logger,
    val errorResolver: ErrorResolver,
    val nextSubscribeStrategy: SubscribeStrategy<A>,
    val subscriptionID: String,
    val initialEventId: String? = null
) : Subscription {

    private val onTransition: StateTransition = { newState ->
        // Safe calls are necessary as there's an edge case initialisation cycle
        // that leads to this being called back when state is being created (still is null).
        // Breaking initialisation and performing actions will be addressed later.
        // For now this is a safe workaround for the issue (#19417)
        // reported by a user (token provider fetch error on init).
        @Suppress("UNNECESSARY_SAFE_CALL")
        logger.verbose("ResumingSubscription $subscriptionID: transitioning " +
                "from ${state?.javaClass?.simpleName} " +
                "to ${newState.javaClass.simpleName}")
        state = newState
    }

    private var state: SubscriptionState = OpeningSubscriptionState(listeners, onTransition)

    override fun unsubscribe() {
        this.state.unsubscribe()
        errorResolver.cancel()
    }

    inner class EndingSubscriptionState : SubscriptionState {
        override fun unsubscribe() {
            logger.verbose("ResumingSubscription $subscriptionID: Subscription is ending; nothing to do for unsubscribe")
        }
    }

    inner class OpeningSubscriptionState(
        listeners: SubscriptionListeners<A>,
        onTransition: StateTransition
    ) : SubscriptionState {
        private lateinit var underlyingSubscription: Subscription

        init {
            var lastEventId = initialEventId

            val subscriptionHeaders = when {
                lastEventId != null -> {
                    logger.verbose("ResumingSubscription $subscriptionID: initialEventId is $lastEventId")
                    headers + ("Last-Event-Id" to listOf(lastEventId))
                }
                else -> headers
            }

            underlyingSubscription = nextSubscribeStrategy(
                SubscriptionListeners<A>(
                    onOpen = { headers ->
                        onTransition(
                            OpenSubscriptionState(
                                listeners,
                                headers,
                                underlyingSubscription,
                                onTransition
                            )
                        )
                    },
                    onSubscribe = listeners.onSubscribe,
                    onEvent = { event ->
                        lastEventId = event.eventId
                        listeners.onEvent(event)
                        logger.verbose(
                            "ResumingSubscription $subscriptionID: received event $event"
                        )
                    },
                    onRetrying = listeners.onRetrying,
                    onError = { error ->
                        onTransition(
                            ResumingSubscriptionState(
                                listeners,
                                error,
                                lastEventId,
                                onTransition
                            )
                        )
                    },
                    onEnd = { error: EOSEvent? ->
                        onTransition(EndedSubscriptionState(listeners, error))
                    }
                ),
                subscriptionHeaders
            )

        }

        override fun unsubscribe() {
            onTransition(EndingSubscriptionState())
            underlyingSubscription.unsubscribe()
        }
    }

    inner class ResumingSubscriptionState(
        val listeners: SubscriptionListeners<A>,
        error: elements.Error,
        lastEventId: String?,
        private val onTransition: StateTransition
    ) : SubscriptionState {
        private var underlyingSubscription: Subscription? = null

        init {
            executeSubscriptionOnce(error, lastEventId)
        }

        override fun unsubscribe() {
            underlyingSubscription?.unsubscribe()
        }

        private fun executeSubscriptionOnce(error: elements.Error, lastEventId: String?) {
            errorResolver.resolveError(error) { resolution ->
                when (resolution) {
                    is DoNotRetry -> onTransition(FailedSubscriptionState(listeners, error))
                    is Retry -> executeNextSubscribeStrategy(lastEventId)
                }
            }
        }

        private fun executeNextSubscribeStrategy(eventId: String?) {
            var lastEventId = eventId

            logger.verbose("ResumingSubscription $subscriptionID: trying to re-establish the subscription")
            val subscriptionHeaders = when {
                lastEventId != null -> {
                    logger.verbose("ResumingSubscription $subscriptionID: initialEventId is $lastEventId")
                    headers + ("Last-Event-Id" to listOf(lastEventId))
                }
                else -> headers
            }

            underlyingSubscription = nextSubscribeStrategy(
                SubscriptionListeners<A>(
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
                    onError = { error ->
                        executeSubscriptionOnce(error, lastEventId)
                    },
                    onEvent = { event ->
                        lastEventId = event.eventId
                        listeners.onEvent(event)
                        logger.verbose(
                            "ResumingSubscription $subscriptionID: received event $event"
                        )
                    },
                    onSubscribe = listeners.onSubscribe,
                    onEnd = { error ->
                        onTransition(EndedSubscriptionState(listeners, error))
                    }
                ),
                subscriptionHeaders
            )
        }
    }

    inner class OpenSubscriptionState(
        listeners: SubscriptionListeners<A>,
        headers: Headers,
        private val underlyingSubscription: Subscription,
        private val onTransition: StateTransition
    ) : SubscriptionState {
        init {
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
    ) : SubscriptionState {
        init {
            listeners.onEnd(error)
        }

        override fun unsubscribe() {
            logger.verbose("ResumingSubscription $subscriptionID: Subscription has already ended; nothing to do for unsubscribe")
        }
    }

    inner class FailedSubscriptionState(
        listeners: SubscriptionListeners<A>,
        error: elements.Error
    ) : SubscriptionState {
        init {
            listeners.onError(error)
        }

        override fun unsubscribe() {
            logger.verbose("ResumingSubscription $subscriptionID: Subscription has already ended; nothing to do for unsubscribe")
        }
    }
}
