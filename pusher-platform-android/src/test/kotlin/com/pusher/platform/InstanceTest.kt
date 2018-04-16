package com.pusher.platform

import org.junit.Test
import org.mockito.stub
import kotlin.test.assertNotNull

class InstanceTest {

    @Test
    fun instanceSetUpCorrectly() {
        val instance = Instance(
            locator = "foo:bar:baz",
            serviceName = "bar",
            serviceVersion = "baz",
            dependencies = TestDependencies()
        )
        assertNotNull(instance)
    }

}

class TestDependencies(androidDependencies: PlatformDependencies = AndroidDependencies(stub())) : PlatformDependencies by androidDependencies {
    override val scheduler: Scheduler = TestScheduler()
    override val mainScheduler: MainThreadScheduler = TestScheduler()
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
