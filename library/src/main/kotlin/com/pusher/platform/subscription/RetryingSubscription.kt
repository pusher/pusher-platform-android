package com.pusher.platform.subscription

import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import android.os.Handler
import com.pusher.platform.ErrorResolver
import com.pusher.platform.retrying.DoNotRetry
import com.pusher.platform.retrying.Retry
import elements.*


fun createRetryingStrategy(
        errorResolver: ErrorResolver,
        nextSubscribeStrategy: SubscribeStrategy,
        logger: Logger): SubscribeStrategy {

    return {
        listeners, headers -> RetryingSubscription(listeners, headers, logger, errorResolver, nextSubscribeStrategy)
    }
}

class RetryingSubscription(listeners: SubscriptionListeners, val headers: Headers, val logger: Logger, val errorResolver: ErrorResolver, val nextSubscribeStrategy: SubscribeStrategy): Subscription {
    var state: SubscriptionState

    val onTransition: StateTransition = { newState ->
        state = newState
    }

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

    inner class OpeningSubscriptionState(listeners: SubscriptionListeners, onTransition: StateTransition) : SubscriptionState {
        lateinit var underlyingSubscription: Subscription

        init {
            logger.verbose("${RetryingSubscription@this}: transitioning to OpeningSubscriptionState")


            underlyingSubscription = nextSubscribeStrategy(
                    SubscriptionListeners(
                            onOpen = {
                                headers -> onTransition(OpenSubscriptionState(listeners, headers, underlyingSubscription, onTransition))
                            },
                            onSubscribe = listeners.onSubscribe,
                            onEvent = listeners.onEvent,
                            onRetrying = listeners.onRetrying,
                            onError = {
                                error -> onTransition(RetryingSubscriptionState(listeners, error, onTransition)) },
                            onEnd = { error: EOSEvent? -> onTransition(EndedSubscriptionState(listeners, error)) }
                    ), headers
            )

        }

        override fun unsubscribe() {
            onTransition(EndingSubscriptionState())
            underlyingSubscription.unsubscribe()
        }
    }

    inner class RetryingSubscriptionState(val listeners: SubscriptionListeners, error: elements.Error, val onTransition: StateTransition) : SubscriptionState {
        var underlyingSubscription: Subscription? = null
        val handler = Handler()

        init {
            logger.verbose("${RetryingSubscription@this}: transitioning to RetryingSubscriptionState")
            executeSubscriptionOnce(error)
        }

        override fun unsubscribe() {
            underlyingSubscription?.unsubscribe()
        }

        private fun executeSubscriptionOnce(error: elements.Error){

            errorResolver.resolveError(error, { resolution ->

                when(resolution){
                    is DoNotRetry -> {
                        onTransition(FailedSubscriptionState(listeners, error))
                    }
                    is Retry -> {
                        executeNextSubscribeStrategy()
                    }
                }
            })
        }

        private fun executeNextSubscribeStrategy(): Unit {
            logger.verbose("${RetryingSubscription@this}: trying to re-establish the subscription")

            underlyingSubscription = nextSubscribeStrategy(
                    SubscriptionListeners(
                            onOpen = {
                                headers  -> onTransition(OpenSubscriptionState(listeners, headers, underlyingSubscription!!, onTransition))
                            },
                            onRetrying = listeners.onRetrying,
                            onError = {
                                error -> executeSubscriptionOnce(error)
                            },
                            onEvent = listeners.onEvent,
                            onSubscribe = listeners.onSubscribe,
                            onEnd = {
                                error -> onTransition(EndedSubscriptionState(listeners, error))
                            }
                    ),
                    headers
            )
        }
    }

    inner class OpenSubscriptionState(listeners: SubscriptionListeners, headers: Headers, val underlyingSubscription: Subscription, onTransition: StateTransition) : SubscriptionState {

        init {
            logger.verbose("${RetryingSubscription@this}: transitioning to OpenSubscriptionState")
            listeners.onOpen(headers)
        }

        override fun unsubscribe() {
            onTransition(EndingSubscriptionState())
            underlyingSubscription.unsubscribe()
        }

    }

    inner class EndedSubscriptionState(listeners: SubscriptionListeners, error: EOSEvent?) : SubscriptionState {

        init {
            logger.verbose("${RetryingSubscription@this}: transitioning to EndedSubscriptionState")
            listeners.onEnd(error)
        }

        override fun unsubscribe() {
            throw Error("Subscription has already ended")
        }

    }

    inner class FailedSubscriptionState(listeners: SubscriptionListeners, error: elements.Error): SubscriptionState {
        init {
            logger.verbose("${RetryingSubscription@this}: transitioning to FailedSubscriptionState")
            listeners.onError(error)
        }

        override fun unsubscribe() {
            throw Error("Subscription has already ended")
        }
    }
}
