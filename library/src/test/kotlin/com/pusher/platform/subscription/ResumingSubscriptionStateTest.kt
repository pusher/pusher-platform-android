package com.pusher.platform.subscription

import android.content.Context
import com.pusher.platform.ErrorResolver
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.network.ConnectivityHelper
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import kotlin.test.assertTrue

class ResumingSubscriptionStateTest {

    var connectivityHelper = mock(ConnectivityHelper::class.java)
    var errorResolver = ErrorResolver(connectivityHelper)
    var listeners = mock(SubscriptionListeners::class.java)

    @Before
    fun setUp(){

    }

    @Test
    fun failingTest(){
        assertTrue { false }
    }






}


