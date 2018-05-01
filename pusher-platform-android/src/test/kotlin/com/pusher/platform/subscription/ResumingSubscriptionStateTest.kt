package com.pusher.platform.subscription

import com.google.gson.JsonElement
import com.nhaarman.mockito_kotlin.*
import com.pusher.platform.ErrorResolver
import com.pusher.platform.RetryStrategyResultCallback
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.retrying.DoNotRetry
import elements.emptyHeaders
import mockitox.stub
import org.junit.jupiter.api.Test

class ResumingSubscriptionStateTest {

    @Test
    fun doNotRetryTransitionsToFailedSubscriptionState(){
        val onTransition = mock<StateTransition>()

        val resumingSub = ResumingSubscription<JsonElement>(SubscriptionListeners(), emptyHeaders(), stub(), neverRetry, stub())
        resumingSub.ResumingSubscriptionState(SubscriptionListeners(), stub(), null, onTransition)

        verify(onTransition).invoke(isA<ResumingSubscription<JsonElement>.FailedSubscriptionState>())
    }

}

private val neverRetry: ErrorResolver = mock {
    on { resolveError(any(), any()) } doAnswer {
        (it.arguments[1] as RetryStrategyResultCallback).invoke(DoNotRetry())
    }
}
