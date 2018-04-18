package com.pusher.platform

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.pusher.platform.network.OkHttpResponsePromise
import com.pusher.platform.network.OkHttpResponseResult
import com.pusher.platform.network.Promise
import com.pusher.platform.test.SyncScheduler
import com.pusher.util.Result
import com.pusher.util.asSuccess
import mockitox.handles
import mockitox.returns
import mockitox.stub
import okhttp3.Response
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
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

        assertThat(tracker).isEqualTo("aaabbbcccdddeeefff")

    }

    @Test
    fun `can copy existing instance`() {

        val expectedResponse = stub<Response>()

        val fakeClient = stub<BaseClient> {
            request(
                requestDestination = RequestDestination.Relative("services/bar/baz/baz/path"),
                headers = emptyMap(),
                method = "GET"
            ) returns Promise.now(expectedResponse.asSuccess())
        }

        val instance = Instance(
            locator = "foo:bar:baz",
            serviceName = "bar",
            serviceVersion = "baz",
            dependencies = InstanceDependencies()
        ).copy(baseClient = fakeClient)

        val request: OkHttpResponsePromise = instance.request(
            options = RequestOptions(path = "path")
        )

        request.onReady { result ->
            assertThat(result.let { it as? Result.Success }?.value).isEqualTo(expectedResponse)
        }
    }

}

class InstanceDependencies(androidDependencies: PlatformDependencies = AndroidDependencies(stub())) : PlatformDependencies by androidDependencies {
    override val scheduler: Scheduler = SyncScheduler()
    override val mainScheduler: MainThreadScheduler = SyncScheduler()
}
