package com.pusher.platform.subscription

import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.tokenProvider.TokenProvider
import elements.*


fun createTokenProvidingStrategy(tokenProvider: TokenProvider? = null, tokenParams: Any? =  null, logger: Logger, nextSubscribeStrategy: SubscribeStrategy): SubscribeStrategy {





    return { listeners, headers ->
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
                                        onOpen = { headers -> onTransition(OpenSubscriptionState(listeners, headers, underlyingSubscription, onTransition))},
                                        onRetrying = listeners.onRetrying,
                                        onError = {
                                            error ->
                                                if(error.tokenExpired()){
                                                    tokenProvider.clearToken(token)
                                                    fetchTokenAndExecuteSubscription()
                                                }
                                                else{
                                                    onTransition(FailedSubscriptionState(error))
                                                }
                                        },
                                        onEvent = listeners.onEvent,
                                        onSubscribe = listeners.onSubscribe,
                                        onEnd = {
                                            error -> onTransition(EndedSubscriptionState(error))
                                        }
                                ),
                                headers
                        )


                    },
                    onFailure = { error -> onTransition(FailedSubscriptionState(error)) }
            )
        }
    }

    inner class OpenSubscriptionState(listeners: SubscriptionListeners, headers: Headers, subscription: Subscription, onTransition: StateTransition): SubscriptionState {
        override fun unsubscribe() {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }

    inner class FailedSubscriptionState(error: Error): SubscriptionState {
        override fun unsubscribe() {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }

    inner class EndedSubscriptionState(error: EOSEvent?): SubscriptionState {
        override fun unsubscribe() {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }
}

private fun Headers.insertToken(token: String) {
    this.put("Authorization", listOf("Bearer $token"))
}

private fun elements.Error.tokenExpired(): Boolean {
    return this is ErrorResponse && this.statusCode == 401 && this.error == "authentication/expired"
}