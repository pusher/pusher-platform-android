package com.pusher.platform

import com.pusher.platform.test.SyncScheduler
import mockitox.stub
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

class InstanceTest {

    @Test
    fun instanceSetUpCorrectly() {
        val instance = Instance(
            locator = "foo:bar:baz",
            serviceName = "bar",
            serviceVersion = "baz",
            dependencies = InstanceDependencies()
        )
        assertNotNull(instance)
    }

}

class InstanceDependencies(androidDependencies: PlatformDependencies = AndroidDependencies(stub())) : PlatformDependencies by androidDependencies {
    override val scheduler: Scheduler = SyncScheduler()
    override val mainScheduler: MainThreadScheduler = SyncScheduler()
}
