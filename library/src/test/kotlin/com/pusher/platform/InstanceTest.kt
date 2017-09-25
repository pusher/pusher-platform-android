package com.pusher.platform

import android.app.Instrumentation
import android.content.Context
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import kotlin.test.assertNotNull
import kotlin.test.fail

class InstanceTest {

    @Mock val context: Context = Mockito.mock(Context::class.java)

    @Before
    fun setUp(){

    }


    @Test
    fun instanceSetUpCorrectly(){

        val instance = Instance( instanceId = "foo:bar:baz", serviceName = "bar", serviceVersion = "baz", context = context)
        assertNotNull(instance)
    }

    @Test
    fun testThatFails(){
        fail("This should fail, all is good.")
    }
}
