package com.pusher.platform

import com.google.common.truth.Truth.assertThat
import com.pusher.SdkInfo
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.ConnectivityHelper
import com.pusher.platform.retrying.RetryStrategyOptions
import com.pusher.platform.test.*
import elements.ErrorResponse
import elements.Subscription
import elements.retryAfter
import mockitox.stub
import org.jetbrains.spek.api.Spek

private const val PATH_10_AND_EOS = "subscribe10"
private const val PATH_3_AND_OPEN = "subscribe_3_continuous"
private const val PATH_0_EOS = "subscribe_0_eos"

private const val HOST = "localhost:10443"

class InstanceIntegrationSpek : Spek({

    describeWhenReachable("https://$HOST", "Instance Subscribe") {
        val instance = Instance(
            locator = "v1:api-ceres:test",
            serviceName = "platform_sdk_tester",
            serviceVersion = "v1",
            dependencies = TestDependencies(),
            baseClient = baseClient
        )

        will("subscribe and terminate on EOS after receiving all events") {
            instance.subscribeNonResuming(
                path = PATH_10_AND_EOS,
                retryOptions = RetryStrategyOptions(limit = 0),
                listeners = listenersWithCounter(
                    onEvent = { events++ },
                    onEnd = { done { assertThat(events).isEqualTo(10) } },
                    onError = { fail("We should not get an error") }
                )
            )
        }

        will("subscribe, terminate on EOS, and trigger onEnd callback exactly once") {
            instance.subscribeNonResuming(
                path = PATH_10_AND_EOS,
                retryOptions = RetryStrategyOptions(limit = 0),
                listeners = listenersWithCounter(
                    onEnd = {
                        end++
                        done { assertThat(end).isEqualTo(1) }
                    },
                    onError = { fail("We should not get an error") }
                )
            )
        }

        will("subscribe to a subscription that is kept open") {
            instance.subscribeNonResuming(
                path = PATH_3_AND_OPEN,
                retryOptions = RetryStrategyOptions(limit = 0),
                listeners = listenersWithCounter(
                    onEvent = {
                        events++
                        attempt { assertThat(events).isLessThan(4) }
                        if (events == 3) done()
                    },
                    onEnd = { fail("onEnd triggered. This shouldn't be!") },
                    onError = { fail("onError triggered - this shouldn't be!") }
                )
            )
        }

        will("subscribe and then unsubscribe - expecting onEnd") {
            var sub: Subscription by FutureValue()
            sub = instance.subscribeNonResuming(
                path = PATH_3_AND_OPEN,
                retryOptions = RetryStrategyOptions(limit = 0),
                listeners = listenersWithCounter(
                    onEvent = {
                        events++
                        attempt { assertThat(events).isLessThan(4) }
                        if (events == 3) {
                            val sub1 = sub
                            sub1.unsubscribe()
                        }
                    },
                    onEnd = { done() },
                    onError = { done { error("onError triggered - this shouldn't be!") } }
                )
            )
        }

        will("subscribe and receive EOS immediately - expecting onEnd with no events") {
            instance.subscribeNonResuming(
                path = PATH_0_EOS,
                retryOptions = RetryStrategyOptions(limit = 0),
                listeners = listenersWithCounter(
                    onEvent = { fail("No events should have been received") },
                    onEnd = { done() },
                    onError = { fail("We should not error") }
                )
            )
        }

        will("subscribe and receive EOS with retry-after headers") {
            instance.subscribeNonResuming(
                path = "subscribe_retry_after",
                retryOptions = RetryStrategyOptions(limit = 0),
                listeners = listenersWithCounter(
                    onEvent = { fail("No events should have been receive") },
                    onEnd = { fail("We should get an error") },
                    onError = { error ->
                        done { assertThat((error as ErrorResponse).headers.retryAfter).isEqualTo(10_000) }
                    }
                )
            )
        }
    }
})

val baseClient = BaseClient(
    host = HOST,
    dependencies = TestDependencies(),
    client = insecureOkHttpClient
)

class TestDependencies : PlatformDependencies {
    override val logger: Logger = object : Logger {
        override fun verbose(message: String, error: Error?) = log("V:", message, error)
        override fun debug(message: String, error: Error?) = log("D:", message, error)
        override fun info(message: String, error: Error?) = log("I:", message, error)
        override fun warn(message: String, error: Error?) = log("W:", message, error)
        override fun error(message: String, error: Error?) = log("E:", message, error)
        private fun log(type: String, message: String, error: Error?) =
            println("$type: $message ${error?.let { "\n" + it } ?: ""}")
    }
    override val mediaTypeResolver: MediaTypeResolver = stub()
    override val connectivityHelper: ConnectivityHelper = AlwaysOnlineConnectivityHelper
    override val sdkInfo: SdkInfo = SdkInfo(
        product = InstanceIntegrationSpek::class.qualifiedName!!,
        language = "Spek",
        platform = "JUnit",
        sdkVersion = "test"
    )
    override val scheduler: Scheduler = AsyncScheduler()
    override val mainScheduler: MainThreadScheduler = AsyncScheduler()
}



