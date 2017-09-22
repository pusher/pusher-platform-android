package com.pusher.platform.subscription

import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.retrying.RetryStrategyOptions
import elements.EOSEvent
import elements.Headers
import elements.Subscription

fun createResumingStrategy(
        retryOptions: RetryStrategyOptions?,
        initialEventId: String? = null,
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
                    logger.verbose("ResumingSubscription: transitioning to EndingSubscriptionState")
                }

                override fun unsubscribe() {
                    throw Error("Subscription is already ending")
                }

            }

            class OpenSubscriptionState(headers: Headers, val underlyingSubscription: Subscription, onTransition: (SubscriptionState) -> Unit) : SubscriptionState {

                init {
                    logger.verbose("ResumingSubscription: transitioning to OpenSubscriptionState")
                    listeners.onOpen(headers)
                }

                override fun unsubscribe() {
                    onTransition(EndingSubscriptionState())
                    underlyingSubscription.unsubscribe()
                }

            }

            class EndedSubscriptionState(error: EOSEvent?) : SubscriptionState {

                init {
                    logger.verbose("ResumingSubscription: transitioning to EndedSubscriptionState")
                    listeners.onEnd(error)
                }

                override fun unsubscribe() {
                    throw Error("Subscription has already ended")
                }

            }

            class FailedSubscriptionState(error: elements.Error): SubscriptionState {
                init {
                    logger.verbose("ResumingSubscription: transitioning to FailedSubscriptionState")
                    listeners.onError(error)
                }

                override fun unsubscribe() {
                    throw Error("Subscription has already ended")
                }
            }

            class ResumingSubscriptionState(error: elements.Error, lastEventId: String?, onTransition: (SubscriptionState) -> Unit) : SubscriptionState {



                override fun unsubscribe() {

                }

            }

            class OpeningSubscriptionState(onTransition: (SubscriptionState) -> Unit) : SubscriptionState {
                lateinit var underlyingSubscription: Subscription

                init {
                    var lastEventId = initialEventId
                    logger.verbose("ResumingSubscription: transitioning to OpeningSubscriptionState")

                    if (lastEventId != null) {
                        headers.put("Last-Event-Id", listOf(lastEventId))
                        logger.verbose("ResumingSubscription: initialEventId is $lastEventId")
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



interface SubscriptionState {
    fun unsubscribe()
}
