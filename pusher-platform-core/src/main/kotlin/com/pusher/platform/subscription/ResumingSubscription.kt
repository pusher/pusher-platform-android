package com.pusher.platform.subscription

import com.pusher.platform.ErrorResolver
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.retrying.DoNotRetry
import com.pusher.platform.retrying.Retry
import elements.EOSEvent
import elements.Headers
import elements.Subscription

fun <A> createResumingStrategy(
    initialEventId: String? = null,
    errorResolver: ErrorResolver,
    nextSubscribeStrategy: SubscribeStrategy<A>,
    logger: Logger
): SubscribeStrategy<A> {
    return { listeners, headers ->
        ResumingSubscription(
            listeners,
            headers,
            logger,
            errorResolver,
            nextSubscribeStrategy,
            initialEventId
        )
    }
}

class ResumingSubscription<A>(
    listeners: SubscriptionListeners<A>,
    val headers: Headers,
    val logger: Logger,
    val errorResolver: ErrorResolver,
    val nextSubscribeStrategy: SubscribeStrategy<A>,
    val initialEventId: String?
) : Subscription {
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
            logger.verbose("${ResumingSubscription@ this}: transitioning to EndingSubscriptionState")
        }

        override fun unsubscribe() {
            throw Error("Subscription is already ending")
        }
    }

    inner class OpeningSubscriptionState(
        listeners: SubscriptionListeners<A>,
        onTransition: StateTransition
    ) : SubscriptionState {
        lateinit var underlyingSubscription: Subscription

        init {
            var lastEventId = initialEventId
            logger.verbose("${ResumingSubscription@ this}: transitioning to OpeningSubscriptionState")

            val subscriptionHeaders = when {
                lastEventId != null -> {
                    logger.verbose("${ResumingSubscription@this}: initialEventId is $lastEventId")
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
                            "${ResumingSubscription@ this}received event ${event}"
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
        var underlyingSubscription: Subscription? = null

        init {
            logger.verbose("${ResumingSubscription@ this}: transitioning to ResumingSubscriptionState")
            executeSubscriptionOnce(error, lastEventId)
        }

        override fun unsubscribe() {
            underlyingSubscription?.unsubscribe()
        }

        private fun executeSubscriptionOnce(error: elements.Error, lastEventId: String?) {
            errorResolver.resolveError(error, { resolution ->
                when (resolution) {
                    is DoNotRetry -> {
                        onTransition(FailedSubscriptionState(listeners, error))
                    }
                    is Retry -> {
                        executeNextSubscribeStrategy(lastEventId)
                    }
                }
            })
        }

        private fun executeNextSubscribeStrategy(eventId: String?) {
            var lastEventId = eventId

            logger.verbose("${ResumingSubscription@ this}: trying to re-establish the subscription")
            val subscriptionHeaders = when {
                lastEventId != null -> {
                    logger.verbose("${ResumingSubscription@ this}: initialEventId is $lastEventId")
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
                            "${ResumingSubscription@ this}received event $event"
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
            logger.verbose("${ResumingSubscription@ this}: transitioning to OpenSubscriptionState")
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
            logger.verbose("${ResumingSubscription@ this}: transitioning to EndedSubscriptionState")
            listeners.onEnd(error)
        }

        override fun unsubscribe() {
            throw Error("Subscription has already ended")
        }
    }

    inner class FailedSubscriptionState(
        listeners: SubscriptionListeners<A>,
        error: elements.Error
    ) : SubscriptionState {
        init {
            logger.verbose("${ResumingSubscription@ this}: transitioning to FailedSubscriptionState")
            listeners.onError(error)
        }

        override fun unsubscribe() {
            throw Error("Subscription has already ended")
        }
    }
}
