package com.pusher.platform

import com.google.common.truth.Truth.assertThat
import com.google.gson.JsonElement
import com.pusher.platform.network.parseAs
import com.pusher.platform.retrying.RetryStrategyOptions
import com.pusher.platform.network.DataParser
import com.pusher.platform.test.describeWhenReachable
import com.pusher.platform.test.listenersWithCounter
import com.pusher.platform.test.will
import elements.ErrorResponse
import elements.Subscription
import elements.retryAfter
import org.jetbrains.spek.api.Spek

private const val PATH_10_AND_EOS = "subscribe10"
private const val PATH_3_AND_OPEN = "subscribe_3_continuous"
private const val PATH_0_EOS = "subscribe_0_eos"
private const val PATH_NOT_EXISTING = "subscribe_missing"
private const val PATH_FORBIDDEN = "subscribe_forbidden"


class InstanceSubscribeIntegrationSpek : Spek({

    describeWhenReachable("https://$HOST", "Instance Subscribe") {
        val instance = Instance(
            locator = "v1:api-ceres:test",
            serviceName = "platform_sdk_tester",
            serviceVersion = "v1",
            dependencies = TestDependencies(),
            baseClient = insecureHttpClient
        )

        will("subscribe and terminate on EOS after receiving all events") {
            instance.subscribeNonResuming(
                path = PATH_10_AND_EOS,
                retryOptions = RetryStrategyOptions(limit = 0),
                bodyParser = JSON_ELEMENT_BODY_PARSER,
                listeners = listenersWithCounter<JsonElement>(
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
                bodyParser = JSON_ELEMENT_BODY_PARSER,
                listeners = listenersWithCounter<JsonElement>(
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
                bodyParser = JSON_ELEMENT_BODY_PARSER,
                listeners = listenersWithCounter<JsonElement>(
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
                bodyParser = JSON_ELEMENT_BODY_PARSER,
                listeners = listenersWithCounter<JsonElement>(
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
                bodyParser = JSON_ELEMENT_BODY_PARSER,
                listeners = listenersWithCounter<JsonElement>(
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
                bodyParser = JSON_ELEMENT_BODY_PARSER,
                listeners = listenersWithCounter<JsonElement>(
                    onEvent = { fail("No events should have been receive") },
                    onEnd = { fail("We should get an error") },
                    onError = { error ->
                        done { assertThat((error as ErrorResponse).headers.retryAfter).isEqualTo(10_000) }
                    }
                )
            )
        }
    }

    describeWhenReachable("https://$HOST", "Instance Subscribe errors nicely") {
        val instance = Instance(
            locator = "v1:api-ceres:test",
            serviceName = "platform_sdk_tester",
            serviceVersion = "v1",
            dependencies = TestDependencies(),
            baseClient = insecureHttpClient
        )

        will("handle 404") {
            instance.subscribeNonResuming(
                path = PATH_NOT_EXISTING,
                retryOptions = RetryStrategyOptions(limit = 0),
                bodyParser = JSON_ELEMENT_BODY_PARSER,
                listeners = listenersWithCounter<JsonElement>(
                    onEvent = { fail("Expecting onError") },
                    onEnd = { fail("Expecting onError") },
                    onError = { error ->
                        done { assertThat((error as ErrorResponse).statusCode).isEqualTo(404) }
                    }
                )
            )
        }

        will("handle 403") {
            instance.subscribeNonResuming(
                path = PATH_FORBIDDEN,
                retryOptions = RetryStrategyOptions(limit = 0),
                bodyParser = JSON_ELEMENT_BODY_PARSER,
                listeners = listenersWithCounter<JsonElement>(
                    onEvent = { fail("Expecting onError") },
                    onEnd = { fail("Expecting onError") },
                    onError = { error ->
                        done { assertThat((error as ErrorResponse).statusCode).isEqualTo(403) }
                    }
                )
            )
        }

        will("handle 500") {
            instance.subscribeNonResuming(
                path = "subscribe_internal_server_error",
                retryOptions = RetryStrategyOptions(limit = 0),
                bodyParser = JSON_ELEMENT_BODY_PARSER,
                listeners = listenersWithCounter<JsonElement>(
                    onEvent = { fail("Expecting onError") },
                    onEnd = { fail("Expecting onError") },
                    onError = { error ->
                        done { assertThat((error as ErrorResponse).statusCode).isEqualTo(500) }
                    }
                )
            )
        }

    }

})


val JSON_ELEMENT_BODY_PARSER: DataParser<JsonElement> = { it.parseAs<JsonElement>() }
