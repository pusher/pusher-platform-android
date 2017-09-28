package com.pusher.platform.subscription

import com.pusher.platform.logger.Logger
import com.pusher.platform.tokenProvider.TokenProvider
import elements.Subscription


fun createTokenProvidingStrategy(tokenProvider: TokenProvider? = null, tokenParams: Any? =  null, logger: Logger, nextSubscribeStrategy: SubscribeStrategy): SubscribeStrategy {





    return { listeners, headers ->
        nextSubscribeStrategy(listeners, headers)
    }


}

class TokenProvidingSubscription: Subscription {

    var state: SubscriptionState

    val onTransition: StateTransition = { newState ->
        state = newState
    }

    override fun unsubscribe(){
        state.unsubscribe()
    }

    init {
        state = TokenProvidingState()
    }

    inner class TokenProvidingState : SubscriptionState {

        override fun unsubscribe() {


        }

    }
}
