package com.pusher.platform.subscription

import com.nhaarman.mockito_kotlin.*
import com.pusher.platform.ErrorResolver
import com.pusher.platform.RetryStrategyResultCallback
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.ConnectivityHelper
import elements.ErrorResponse
import elements.Subscription
import org.junit.Before
import org.junit.Test
import java.util.*
import kotlin.test.assertTrue

class ResumingSubscriptionStateTest {

    var connectivityHelper: ConnectivityHelper = mock()
    var errorResolver: ErrorResolver = mock()
    var listeners = SubscriptionListeners()
    var headers = TreeMap<String, List<String>>()
    var logger: Logger = mock()

    var nextSubscribeStrategy: SubscribeStrategy = mock()
    var onTransition = mock<StateTransition>()

    @Before
    fun setUp(){

    }


    @Test
    fun doNotRetryTransitionsToFailedSubscriptionState(){

        val error = mock<ErrorResponse>()

        doAnswer {
            (it.arguments[1] as RetryStrategyResultCallback).invoke(DoNotRetry())
        }.`when`(errorResolver).resolveError(any<elements.Error>(), any<(RetryStrategyResultCallback)>())

        val resumingSub = ResumingSubscription(listeners, headers, logger, errorResolver, nextSubscribeStrategy,  null)
        resumingSub.ResumingSubscriptionState(listeners, error, null, onTransition)

        verify(onTransition).invoke(isA<ResumingSubscription.FailedSubscriptionState>())
    }



}


