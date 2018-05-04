package com.pusher.platform

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.pusher.platform.network.DataParser
import com.pusher.platform.network.Futures
import com.pusher.util.Result
import com.pusher.util.asSuccess
import elements.Error
import mockitox.returns
import mockitox.stub
import org.junit.jupiter.api.Test
import java.util.concurrent.Future
import kotlin.test.assertNotNull

class InstanceTest {

    @Test
    fun `instance set up correctly`() {
        val instance = Instance(
            locator = "foo:bar:baz",
            serviceName = "bar",
            serviceVersion = "baz",
            dependencies = AndroidDependencies()
        )
        assertNotNull(instance)
    }

    @Test
    fun `composition of multiple listeners`() {
        var tracker = ""
        val subscription = SubscriptionListeners.compose<String>(
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

        val expectedResponse = stub<JsonElement>()

        val fakeClient = stub<BaseClient> {
            request(
                requestDestination = RequestDestination.Relative("services/bar/baz/baz/path"),
                headers = emptyMap(),
                method = "GET",
                responseParser = jsonParser
            ) returns Futures.now(expectedResponse.asSuccess())
        }

        val instance = Instance(
            locator = "foo:bar:baz",
            serviceName = "bar",
            serviceVersion = "baz",
            dependencies = AndroidDependencies()
        ).copy(baseClient = fakeClient)

        val request: Future<Result<JsonElement, Error>> = instance.request(
            options = RequestOptions(path = "path"),
            responseParser = jsonParser
        )

        assertThat(request.get().let { it as? Result.Success }?.value).isEqualTo(expectedResponse)
    }

}

private val jsonParser: DataParser<JsonElement> = { Gson().fromJson(it, JsonElement::class.java).asSuccess() }
