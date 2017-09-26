package com.pusher.platform.subscription

import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.retrying.RetryStrategyOptions
import android.os.Handler
import com.pusher.platform.ErrorResolver
import elements.*


fun createResumingStrategy(
        retryOptions: RetryStrategyOptions?,
        initialEventId: String? = null,
        errorResolver: ErrorResolver,
        nextSubscribeStrategy: SubscribeStrategy,
        logger: Logger): SubscribeStrategy {

    class ResumingSubscription(listeners: SubscriptionListeners, headers: Headers): Subscription{
        var state: SubscriptionState

        val onTransition: (SubscriptionState) -> Unit = {newState ->
            this.state = newState
        }

        init {
            class EndingSubscriptionState : SubscriptionState {
                init {
                    logger.verbose("${ResumingSubscription@this}: transitioning to EndingSubscriptionState")
                }

                override fun unsubscribe() {
                    throw Error("Subscription is already ending")
                }

            }

            class OpenSubscriptionState(headers: Headers, val underlyingSubscription: Subscription, onTransition: (SubscriptionState) -> Unit) : SubscriptionState {

                init {
                    logger.verbose("${ResumingSubscription@this}: transitioning to OpenSubscriptionState")
                    listeners.onOpen(headers)
                }

                override fun unsubscribe() {
                    onTransition(EndingSubscriptionState())
                    underlyingSubscription.unsubscribe()
                }

            }

            class EndedSubscriptionState(error: EOSEvent?) : SubscriptionState {

                init {
                    logger.verbose("${ResumingSubscription@this}: transitioning to EndedSubscriptionState")
                    listeners.onEnd(error)
                }

                override fun unsubscribe() {
                    throw Error("Subscription has already ended")
                }

            }

            class FailedSubscriptionState(error: elements.Error): SubscriptionState {
                init {
                    logger.verbose("${ResumingSubscription@this}: transitioning to FailedSubscriptionState")
                    listeners.onError(error)
                }

                override fun unsubscribe() {
                    throw Error("Subscription has already ended")
                }
            }

            class ResumingSubscriptionState(error: elements.Error, lastEventId: String?, val onTransition: (SubscriptionState) -> Unit) : SubscriptionState {
                var underlyingSubscription: Subscription? = null
                val handler = Handler()

                init {
                    logger.verbose("${ResumingSubscription@this}: transitioning to ResumingSubscriptionState")
                    executeSubscriptionOnce(error, lastEventId)
                }

                override fun unsubscribe() {
                    underlyingSubscription?.unsubscribe()
                }

                private fun executeSubscriptionOnce(error: elements.Error, lastEventId: String?){

                    errorResolver.resolveError(error, { resolution ->

                        when(resolution){
                            is DoNotRetry -> {
                                onTransition(FailedSubscriptionState(error))
                            }
                            is Retry -> {
                                handler.postDelayed({ executeNextSubscribeStrategy(lastEventId) }, resolution.waitTimeMillis)
                            }
                        }
                    })
                }

                private fun executeNextSubscribeStrategy(eventId: String?): Unit {

                    var lastEventId = eventId

                    logger.verbose("${ResumingSubscription@this}: trying to re-establish the subscription")
                    if(lastEventId != null){
                        headers.put("Last-Event-Id", listOf(lastEventId!!))
                        logger.verbose("${ResumingSubscription@this}: initialEventId is $lastEventId")
                    }


                    underlyingSubscription = nextSubscribeStrategy(
                            SubscriptionListeners(
                                    onOpen = {
                                        headers  -> onTransition(OpenSubscriptionState(headers, underlyingSubscription!!, onTransition))
                                    },
                                    onRetrying = listeners.onRetrying,
                                    onError = {
                                        error -> executeSubscriptionOnce(error, lastEventId)
                                    },
                                    onEvent = {
                                        event -> lastEventId = event.eventId
                                    },
                                    onSubscribe = listeners.onSubscribe,
                                    onEnd = {
                                        error -> onTransition(EndedSubscriptionState(error))
                                    }
                            ),
                            headers
                    )
                }
            }

            class OpeningSubscriptionState(onTransition: (SubscriptionState) -> Unit) : SubscriptionState {
                lateinit var underlyingSubscription: Subscription

                init {
                    var lastEventId = initialEventId
                    logger.verbose("${ResumingSubscription@this}: transitioning to OpeningSubscriptionState")

                    if (lastEventId != null) {
                        headers.put("Last-Event-Id", listOf(lastEventId!!))
                        logger.verbose("${ResumingSubscription@this}: initialEventId is $lastEventId")
                    }

                    underlyingSubscription = nextSubscribeStrategy(
                            SubscriptionListeners(
                                    onOpen = {
                                        headers -> onTransition(OpenSubscriptionState(headers, underlyingSubscription, onTransition))
                                    },
                                    onSubscribe = listeners.onSubscribe,
                                    onEvent = {
                                        event -> lastEventId = event.eventId
                                        listeners.onEvent(event)
                                    },
                                    onRetrying = listeners.onRetrying,
                                    onError = { error -> onTransition(ResumingSubscriptionState(error, lastEventId, onTransition)) },
                                    onEnd = { error: EOSEvent? -> onTransition(EndedSubscriptionState(error)) }
                            ), headers
                    )

                }

                override fun unsubscribe() {
                    onTransition(EndingSubscriptionState())
                    underlyingSubscription.unsubscribe()
                }
            }

            state = OpeningSubscriptionState(onTransition)
        }


        override fun unsubscribe() {
            this.state.unsubscribe()
        }
    }

    return {
        listeners, headers -> ResumingSubscription(listeners, headers)
    }
}

sealed class RetryStrategyResult

data class Retry(val waitTimeMillis: Long): RetryStrategyResult()

class DoNotRetry: RetryStrategyResult()


interface SubscriptionState {
    fun unsubscribe()
}
