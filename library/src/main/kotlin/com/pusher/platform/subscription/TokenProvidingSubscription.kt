package com.pusher.platform.subscription

import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.tokenProvider.TokenProvider
import elements.*


fun createTokenProvidingStrategy(
        tokenProvider: TokenProvider? = null,
        tokenParams: Any? =  null,
        logger: Logger,
        nextSubscribeStrategy: SubscribeStrategy): SubscribeStrategy {

    if(tokenProvider != null) {
        return { listeners, headers -> TokenProvidingSubscription(listeners, headers, tokenProvider, tokenParams, logger, nextSubscribeStrategy) }
    }
    else return { listeners, headers ->
        nextSubscribeStrategy(listeners, headers)
    }
}

class TokenProvidingSubscription(listeners: SubscriptionListeners, val headers: Headers, val tokenProvider: TokenProvider, val tokenParams: Any? = null, val logger: Logger, val nextSubscribeStrategy: SubscribeStrategy): Subscription {

    var state: SubscriptionState

    val onTransition: StateTransition = { newState ->
        state = newState
    }

    override fun unsubscribe(){
        state.unsubscribe()
    }

    init {
        state = TokenProvidingState(listeners, onTransition)
    }

    inner class TokenProvidingState(val listeners: SubscriptionListeners, onTransition: StateTransition) : SubscriptionState {

        lateinit var underlyingSubscription: Subscription
        init {
            logger.verbose("${TokenProvidingSubscription@this}: Transitioning to TokenProvidingState")
            fetchTokenAndExecuteSubscription()
        }

        override fun unsubscribe() {
            TODO()
        }


        fun fetchTokenAndExecuteSubscription() {
            tokenProvider.fetchToken(
                    tokenParams = tokenParams,
                    onSuccess = { token ->
                        headers.insertToken(token)
                        logger.verbose("${TokenProvidingSubscription@this}: token fetched: $token")
                        underlyingSubscription = nextSubscribeStrategy(
                                SubscriptionListeners(
                                        onOpen = { headers -> onTransition(OpenSubscriptionState(headers, listeners, underlyingSubscription, onTransition))},
                                        onRetrying = listeners.onRetrying,
                                        onError = {
                                            error ->
                                                if(error.tokenExpired()){
                                                    tokenProvider.clearToken(token)
                                                    fetchTokenAndExecuteSubscription()
                                                }
                                                else{
                                                    onTransition(FailedSubscriptionState(listeners, error))
                                                }
                                        },
                                        onEvent = listeners.onEvent,
                                        onSubscribe = listeners.onSubscribe,
                                        onEnd = {
                                            error -> onTransition(EndedSubscriptionState(listeners, error))
                                        }
                                ),
                                headers
                        )


                    },
                    onFailure = { error -> onTransition(FailedSubscriptionState(listeners, error)) }
            )
        }
    }

    inner class OpenSubscriptionState(headers: Headers, val listeners: SubscriptionListeners, val underlyingSubscription: Subscription, val onTransition: StateTransition): SubscriptionState {
        init {
            logger.verbose("${TokenProvidingSubscription@this}: Transitioning to OpenSubscriptionState")
            listeners.onOpen(headers)
        }
        override fun unsubscribe() {
            underlyingSubscription.unsubscribe()
            onTransition(EndedSubscriptionState(listeners))
        }
    }

    inner class FailedSubscriptionState(listeners: SubscriptionListeners, error: Error): SubscriptionState {
        init {
            logger.verbose("${TokenProvidingSubscription@this}: Transitioning to FailedSubscriptionState")
            listeners.onError(error)
        }
        override fun unsubscribe() {
            throw Error("Subscription has already ended")
        }
    }

    inner class EndedSubscriptionState(listeners: SubscriptionListeners, error: EOSEvent? = null): SubscriptionState {
        init {
            logger.verbose("${TokenProvidingSubscription@this}: Transitioning to EndedSubscriptionState")
            listeners.onEnd(error)
        }
        override fun unsubscribe() {
            throw Error("Subscription has already ended")
        }
    }
}

private fun Headers.insertToken(token: String) {
    this.put("Authorization", listOf("Bearer $token"))
}

private fun elements.Error.tokenExpired(): Boolean {
    return this is ErrorResponse && this.statusCode == 401 && this.error == "authentication/expired"
}