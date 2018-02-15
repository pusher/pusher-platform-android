package com.pusher.platform

import android.content.Context
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import kotlin.test.assertNotNull

class InstanceTest {

    @Mock val context: Context = Mockito.mock(Context::class.java)
    private val scheduler = TestScheduler()

    @Before
    fun setUp(){

    }


    @Test
    fun instanceSetUpCorrectly(){
        val instance = AndroidInstance(
            locator = "foo:bar:baz",
            serviceName = "bar",
            serviceVersion = "baz",
            context = context,
            backgroundScheduler = scheduler,
            foregroundScheduler = scheduler
        )
        assertNotNull(instance)
    }

}

class TestScheduler : MainThreadScheduler {
    override fun schedule(action: () -> Unit): ScheduledJob {
        action()
        return TestScheduleJob()
    }

    override fun schedule(delay: Long, action: () -> Unit): ScheduledJob {
        action() // no delay in tests
        return TestScheduleJob()
    }

}

class TestScheduleJob : ScheduledJob {

    var canceled: Boolean = false

    override fun cancel() {
        canceled = true
    }
}
