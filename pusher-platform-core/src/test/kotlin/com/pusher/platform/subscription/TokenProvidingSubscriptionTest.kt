package com.pusher.platform.subscription

import com.google.common.truth.Truth.assertThat
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.Futures
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.asSuccess
import elements.Subscription
import org.junit.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.any
import org.mockito.Mockito.mock

internal class TokenProvidingSubscriptionTest {

    @Test
    fun `allows to unsubscribe`() {
        val future = Futures.schedule { Thread.sleep(100); "".asSuccess<String, elements.Error>() }

        val subscription = TokenProvidingSubscription(
            logger = mock(Logger::class.java),
            listeners = SubscriptionListeners<String>(),
            headers = emptyMap(),
            tokenProvider = mock(TokenProvider::class.java).apply {
                given(this.fetchToken(any())).willReturn(future)
            },
            tokenParams = null,
            nextSubscribeStrategy = { _, _ -> mock(Subscription::class.java) },
            subscriptionID = "test"
        )

        subscription.unsubscribe()

        assertThat(future.isCancelled).isTrue()
    }
}
