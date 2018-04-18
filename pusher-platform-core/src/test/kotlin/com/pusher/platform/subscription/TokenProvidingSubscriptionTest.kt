package com.pusher.platform.subscription

import com.google.common.truth.Truth.assertThat
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.Promise
import com.pusher.platform.tokenProvider.TokenProvider
import elements.Subscription
import org.junit.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.any
import org.mockito.Mockito.mock

internal class TokenProvidingSubscriptionTest {

    @Test
    fun `allows to unsubscribe`() {
        var cancelled = false

        val subscription = TokenProvidingSubscription(
            logger = mock(Logger::class.java),
            listeners = SubscriptionListeners(),
            headers = emptyMap(),
            tokenProvider = mock(TokenProvider::class.java).apply {
                given(this.fetchToken(any())).willReturn(Promise.promise {
                    onCancel { cancelled = true }
                })
            },
            tokenParams = null,
            nextSubscribeStrategy = { _, _ -> mock(Subscription::class.java) }
        )

        subscription.unsubscribe()
        assertThat(cancelled).isTrue()

    }

}