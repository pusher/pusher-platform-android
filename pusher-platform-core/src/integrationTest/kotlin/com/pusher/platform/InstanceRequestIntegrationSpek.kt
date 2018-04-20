//package com.pusher.platform
//
//import com.google.common.truth.Truth.assertThat
//import com.pusher.platform.network.Promise
//import com.pusher.platform.network.asPromise
//import com.pusher.platform.network.parseAs
//import com.pusher.platform.test.SuspendedTestBody
//import com.pusher.platform.test.describeWhenReachable
//import com.pusher.platform.test.will
//import com.pusher.platform.tokenProvider.TokenProvider
//import com.pusher.util.Result
//import com.pusher.util.asSuccess
//import elements.Error
//import elements.ErrorResponse
//import okhttp3.Response
//import org.jetbrains.spek.api.Spek
//import org.jetbrains.spek.api.dsl.describe
//
//class InstanceRequestIntegrationSpek : Spek({
//
//
//    describeWhenReachable("https://${HOST}", "Instance Requests") {
//        val instance = Instance(
//            locator = "v1:api-ceres:test",
//            serviceName = "platform_sdk_tester",
//            serviceVersion = "v1",
//            dependencies = TestDependencies(),
//            baseClient = insecureHttpClient
//        )
//
//        will("make a successful GET request") {
//            instance.request(
//                options = RequestOptions("get_ok")
//            ).onReady(::assertSuccess)
//        }
//
//        will("make a successful POST request") {
//            instance.request(
//                options = RequestOptions(
//                    method = "POST",
//                    path = "post_ok"
//                )
//            ).onReady(::assertSuccess)
//        }
//
//        will("make a successful POST request with JSON body") {
//            val expected: Result<Map<String, String>, Error> = mapOf("test" to "123").asSuccess()
//            instance.request(
//                options = RequestOptions(
//                    method = "POST",
//                    path = "post_ok",
//                    body = """{ "test": "123" }"""
//                )
//            ).onReady { result ->
//                done {
//                    val responseBody = (result as Result.Success).value.body()
//                    val rawResult = responseBody?.charStream()
//                    val success = rawResult?.parseAs<Map<String, String>>() as Result.Success
//                    responseBody.close()
//                    assertThat(success).isEqualTo(expected)
//                }
//            }
//        }
//
//        will("make a successful PUT request") {
//            instance.request(
//                options = RequestOptions(
//                    method = "PUT",
//                    path = "put_ok"
//                )
//            ).onReady(::assertSuccess)
//        }
//
//        will("make a successful DELETE request") {
//            instance.request(
//                options = RequestOptions(
//                    method = "DELETE",
//                    path = "delete_ok"
//                )
//            ).onReady(::assertSuccess)
//        }
//
//    }
//
//    describeWhenReachable("https://${HOST}", "Instance requests - failing") {
//
//        val instance = Instance(
//            locator = "v1:api-ceres:test",
//            serviceName = "platform_sdk_tester",
//            serviceVersion = "v1",
//            dependencies = TestDependencies(),
//            baseClient = insecureHttpClient
//        )
//
//        describe("with no token provider") {
//
//            forEachErrorCode { errorCode ->
//                will("fail on $errorCode error") {
//                    instance.request(
//                        options = RequestOptions("get_$errorCode")
//                    ).onReady { result ->
//                        result.fold(
//                            onFailure = { error ->
//                                done { assertThat((error as ErrorResponse).statusCode).isEqualTo(errorCode) }
//                            },
//                            onSuccess = { fail("Expecting error") }
//                        )
//                    }
//                }
//            }
//        }
//
//        describe("with a token provider") {
//
//            val dummyTokenProvider: TokenProvider = object : TokenProvider {
//                override fun fetchToken(tokenParams: Any?): Promise<Result<String, Error>> =
//                    "blahblaharandomtoken".asSuccess<String, Error>().asPromise()
//
//                override fun clearToken(token: String?) = Unit
//            }
//
//            forEachErrorCode { errorCode ->
//                will("fail on $errorCode error") {
//                    instance.request(
//                        options = RequestOptions("get_$errorCode"),
//                        tokenProvider = dummyTokenProvider
//                    ).onReady { result ->
//                        result.fold(
//                            onFailure = { error ->
//                                done { assertThat((error as ErrorResponse).statusCode).isEqualTo(errorCode) }
//                            },
//                            onSuccess = { fail("Expecting error") }
//                        )
//                    }
//                }
//            }
//
//        }
//
//    }
//
//})
//
//
//private fun forEachErrorCode(testBody: (Int) -> Unit) =
//    arrayOf(400, 403, 404, 500, 503).forEach(testBody)
//
//private fun SuspendedTestBody.assertSuccess(result: Result<Response, Error>) {
//    result.fold(
//        onFailure = { fail(it.reason) },
//        onSuccess = { response -> done { response.close() } }
//    )
//}
