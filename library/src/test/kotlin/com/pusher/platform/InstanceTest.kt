package com.pusher.platform

import org.junit.Before
import org.junit.Test
import kotlin.test.assertNotNull

class InstanceTest {

    @Before
    fun setUp(){

    }


    @Test
    fun instanceSetUpCorrectly(){

        val instance = Instance( instanceId = "foo:bar:baz", serviceName = "bar", serviceVersion = "baz")
        assertNotNull(instance)
    }
}
