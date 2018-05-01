package com.pusher.platform.subscription

import com.google.gson.JsonElement
import com.nhaarman.mockito_kotlin.*
import com.pusher.platform.ErrorResolver
import com.pusher.platform.RetryStrategyResultCallback
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.retrying.DoNotRetry
import elements.ErrorResponse
import org.junit.jupiter.api.Test
import java.util.*

class ResumingSubscriptionStateTest {

    var errorResolver: ErrorResolver = mock {
        on { resolveError(any(), any()) } doAnswer {
            (it.arguments[1] as RetryStrategyResultCallback).invoke(DoNotRetry())
        }
    }
    var listeners = SubscriptionListeners<JsonElement>()
    var headers = TreeMap<String, List<String>>()
    var logger: Logger = mock()

    var nextSubscribeStrategy: SubscribeStrategy<JsonElement> = mock()
    var onTransition = mock<StateTransition>()

    @Test
    fun doNotRetryTransitionsToFailedSubscriptionState(){

        val error = mock<ErrorResponse>()

        val resumingSub = ResumingSubscription(
            listeners,
            headers,
            logger,
            errorResolver,
            nextSubscribeStrategy
        )
        resumingSub.ResumingSubscriptionState(listeners, error, null, onTransition)

        verify(onTransition).invoke(isA<ResumingSubscription<JsonElement>.FailedSubscriptionState>())
    }



}


