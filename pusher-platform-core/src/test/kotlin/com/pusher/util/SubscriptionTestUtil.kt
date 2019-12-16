package com.pusher.util

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.pusher.platform.SubscriptionListeners
import elements.Error
import org.mockito.Mockito

fun errorPartialMockListener(errorCallback: (Error) -> Unit): SubscriptionListeners<String> =
        mock(defaultAnswer = Mockito.RETURNS_SMART_NULLS) {
            on { onError } doReturn (errorCallback)
        }