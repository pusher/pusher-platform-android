package com.pusher.platform

import com.google.common.truth.Truth.assertThat
import com.pusher.SdkInfo
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.ConnectivityHelper
import com.pusher.platform.retrying.RetryStrategyOptions
import com.pusher.platform.test.*
import elements.ErrorResponse
import elements.Subscription
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
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
                path = PATH_3_AND_OPEN,
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
                        if (events == 3) sub.unsubscribe()
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
                        done {
                            assertThat((error as ErrorResponse).headers)
                                .containsEntry("Retry-After", listOf("10"))
                        }
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
    override val logger: Logger = stub()
    override val mediaTypeResolver: MediaTypeResolver = stub()
    override val connectivityHelper: ConnectivityHelper = AlwaysOnlineConnectivityHelper
    override val sdkInfo: SdkInfo = SdkInfo(
        product = InstanceIntegrationSpek::class.qualifiedName!!,
        language = "Spek",
        platform = "JUnit",
        sdkVersion = "test"
    )
    override val scheduler: Scheduler = SyncScheduler()
    override val mainScheduler: MainThreadScheduler = SyncScheduler()
}



