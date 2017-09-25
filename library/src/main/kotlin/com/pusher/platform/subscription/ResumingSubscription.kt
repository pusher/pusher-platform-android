package com.pusher.platform.subscription

import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.retrying.RetryStrategyOptions
import android.os.Handler
import com.pusher.platform.network.ConnectivityHelper
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

            class ResumingSubscriptionState(error: elements.Error, var lastEventId: String?, onTransition: (SubscriptionState) -> Unit) : SubscriptionState {
                lateinit var underlyingSubscription: Subscription

                init {
                    logger.verbose("${ResumingSubscription@this}: transitioning to ResumingSubscriptionState")

                    val executeSubscriptionOnce: (elements.Error, String?) -> Unit = { error, lastEventId ->

                        errorResolver.resolveError(error, { resolution ->

                            //TODO:

                        })






                    }

                    val executeNextSubscribeStrategy: (String?) -> Unit = { eventId ->

                        var lastEventId = eventId

                        logger.verbose("${ResumingSubscription@this}: trying to re-establish the subscription")
                        if(lastEventId != null){
                            headers.put("Last-Event-Id", listOf(lastEventId!!))
                            logger.verbose("${ResumingSubscription@this}: initialEventId is $lastEventId")
                        }


                        underlyingSubscription = nextSubscribeStrategy(
                                SubscriptionListeners(
                                        onOpen = {
                                            headers  -> onTransition(OpenSubscriptionState(headers, underlyingSubscription, onTransition))
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



                    executeSubscriptionOnce(error, lastEventId)
                }




                override fun unsubscribe() {

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

class ErrorResolver(val connectivityHelper: ConnectivityHelper) {

    var errorBeingResolved: Any = {}
    val handler = Handler()
    var retryNow: (() -> Unit)? = null

    fun resolveError(error: elements.Error, callback: (RetryStrategyResult) -> Unit){

        when(error){
            is NetworkError -> {
                retryNow = { callback(Retry() )}
                connectivityHelper.onConnected(retryNow!!)
            }
            is ErrorResponse -> {

                //Retry-After present
                if (error.headers["Retry-After"] != null) {
                    val retryAfter = error.headers["Retry-After"]!![0].toLong() * 1000
                    retryNow = { callback(Retry() )}
                    handler.postDelayed(retryNow, retryAfter)
                }

                //Retry-After NOT present

            }
        }


        //network error - > use conn helper

        //error has a Retry-After -> wait that long

        //error has a generic error -> use exponential backoff
    }


    fun cancel() {
        if(retryNow != null){
            handler.removeCallbacks(retryNow)
        }

    }

}

sealed class RetryStrategyResult

class Retry: RetryStrategyResult()

class DoNotRetry: RetryStrategyResult()


interface SubscriptionState {
    fun unsubscribe()
}
