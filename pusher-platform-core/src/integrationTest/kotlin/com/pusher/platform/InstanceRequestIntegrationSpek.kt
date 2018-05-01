package com.pusher.platform

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.pusher.platform.network.toFuture
import com.pusher.platform.network.wait
import com.pusher.platform.test.describeWhenReachable
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.Result
import com.pusher.util.ResultAssertions.assertFailure
import com.pusher.util.ResultAssertions.assertSuccess
import com.pusher.util.asSuccess
import elements.Error
import elements.ErrorResponse
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.util.concurrent.Future

class InstanceRequestIntegrationSpek : Spek({

    describeWhenReachable("https://$HOST", "Instance Requests") {
        val instance = Instance(
            locator = "v1:api-ceres:test",
            serviceName = "platform_sdk_tester",
            serviceVersion = "v1",
            dependencies = TestDependencies(),
            baseClient = insecureHttpClient
        )

        it("makes a successful GET request") {
            val result = instance.request<JsonElement>(
                options = RequestOptions("get_ok")
            ).wait()
            assertSuccess(result)
        }

        it("makes a successful POST request") {
            val result = instance.request<JsonPrimitive>(
                options = RequestOptions(
                    method = "POST",
                    path = "post_ok"
                )
            ).wait()
            assertSuccess(result)
        }

        it("makes a successful POST request with JSON body") {
            val expected = mapOf("test" to "123")
            val result = instance.request<Map<String, String>>(
                options = RequestOptions(
                    method = "POST",
                    path = "post_ok",
                    body = """{ "test": "123" }"""
                )
            ).wait()

            assertSuccess(result).isEqualTo(expected)
        }

        it("makes a successful PUT request") {
            val result = instance.request<JsonElement>(
                options = RequestOptions(
                    method = "PUT",
                    path = "put_ok"
                )
            ).wait()
            assertSuccess(result)
        }

        it("makes a successful DELETE request") {
            val result = instance.request<JsonElement>(
                options = RequestOptions(
                    method = "DELETE",
                    path = "delete_ok"
                )
            ).wait()
            assertSuccess(result)
        }

    }

    describeWhenReachable("https://$HOST", "Instance requests - failing") {

        val instance = Instance(
            locator = "v1:api-ceres:test",
            serviceName = "platform_sdk_tester",
            serviceVersion = "v1",
            dependencies = TestDependencies(),
            baseClient = insecureHttpClient
        )

        describe("with no token provider") {

            forEachErrorCode { errorCode ->
                it("fail on $errorCode error") {
                    val result = instance.request<JsonElement>(
                        options = RequestOptions("get_$errorCode")
                    ).wait()

                    assertFailure(result)
                    check(result.let { it as Result.Failure }.error.let { it as ErrorResponse }.statusCode == errorCode)
                }
            }
        }

        describe("with a token provider") {

            val dummyTokenProvider: TokenProvider = object : TokenProvider {
                override fun fetchToken(tokenParams: Any?): Future<Result<String, Error>> =
                    "blahblaharandomtoken".asSuccess<String, Error>().toFuture()

                override fun clearToken(token: String?) = Unit
            }

            forEachErrorCode { errorCode ->
                it("fails on $errorCode error") {
                    val result = instance.request<JsonElement>(
                        options = RequestOptions("get_$errorCode"),
                        tokenProvider = dummyTokenProvider
                    ).wait()

                    assertFailure(result)
                    check(result.let { it as Result.Failure }.error.let { it as ErrorResponse }.statusCode == errorCode)
                }
            }

        }

    }

})


private fun forEachErrorCode(testBody: (Int) -> Unit) =
    arrayOf(400, 403, 404, 500, 503).forEach(testBody)

