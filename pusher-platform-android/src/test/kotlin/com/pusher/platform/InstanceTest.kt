package com.pusher.platform

import com.google.common.truth.Truth
import com.pusher.platform.test.SyncScheduler
import mockitox.stub
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

class InstanceTest {

    @Test
    fun `instance set up correctly`() {
        val instance = Instance(
            locator = "foo:bar:baz",
            serviceName = "bar",
            serviceVersion = "baz",
            dependencies = InstanceDependencies()
        )
        assertNotNull(instance)
    }

    @Test
    fun `composition of multiple listeners`() {
        var tracker = ""
        val subscription = SubscriptionListeners.compose(
            SubscriptionListeners(
                onEnd = { tracker += "a" },
                onOpen = { tracker += "b" },
                onError = { tracker += "c" },
                onEvent = { tracker += "d" },
                onSubscribe = { tracker += "e" },
                onRetrying = { tracker += "f" }
            ),
            SubscriptionListeners(
                onEnd = { tracker += "aa" },
                onOpen = { tracker += "bb" },
                onError = { tracker += "cc" },
                onEvent = { tracker += "dd" },
                onSubscribe = { tracker += "ee" },
                onRetrying = { tracker += "ff" }
            )
        )

        subscription.onEnd(stub())
        subscription.onOpen(stub())
        subscription.onError(stub())
        subscription.onEvent(stub())
        subscription.onSubscribe()
        subscription.onRetrying()

        Truth.assertThat(tracker).isEqualTo("aaabbbcccdddeeefff")

    }

}

class InstanceDependencies(androidDependencies: PlatformDependencies = AndroidDependencies(stub())) : PlatformDependencies by androidDependencies {
    override val scheduler: Scheduler = SyncScheduler()
    override val mainScheduler: MainThreadScheduler = SyncScheduler()
}
