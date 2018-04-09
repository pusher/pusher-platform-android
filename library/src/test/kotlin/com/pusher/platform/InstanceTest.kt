package com.pusher.platform

import android.content.Context
import com.pusher.platform.test.SyncScheduler
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import kotlin.test.assertNotNull

class InstanceTest {

    private val context: Context = Mockito.mock(Context::class.java)

    @Test
    fun instanceSetUpCorrectly(){
        val instance = AndroidInstance(
            locator = "foo:bar:baz",
            serviceName = "bar",
            serviceVersion = "baz",
            context = context,
            backgroundScheduler = SyncScheduler(),
            foregroundScheduler = SyncScheduler()
        )
        assertNotNull(instance)
    }

}
