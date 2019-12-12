package com.pusher.platform

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.pusher.platform.network.DataParser
import com.pusher.platform.retrying.RetryStrategyOptions
import com.pusher.util.asSuccess
import com.pusher.util.errorPartialMockListener
import com.pusher.util.mockFailingTokenProvider
import elements.Error
import elements.Errors
import elements.emptyHeaders
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class DummyBaseClientSpec : Spek({

    val subject: BaseClient by memoized {
        BaseClient("dummy", TestDependencies())
    }

    describe("given 401 failing token provider") {
        val dummyRequestDestination = RequestDestination.Relative("dummy") // irrelevant here
        val emptyRequestHeaders = emptyHeaders() // also irrelevant for this test
        val auth401Error = Errors.response(401, emptyHeaders(), "test error body")
        val mockFailingTokenProvider by memoized { mockFailingTokenProvider(auth401Error) }
        val mockErrorCallback by memoized { mock<(Error) -> Unit>() }
        val defaultRetryStrategyOptions = RetryStrategyOptions() // effectively no retrying
        val dummyBodyParser: DataParser<String> = { "".asSuccess() } // irrelevant for this test
        val errorPartialMockListener by memoized { errorPartialMockListener(mockErrorCallback) }

        describe("when subscribeResuming is called") {
            beforeEachTest {
                subject.subscribeResuming(
                        destination = dummyRequestDestination,
                        headers = emptyRequestHeaders,
                        tokenProvider = mockFailingTokenProvider,
                        tokenParams = null,
                        retryOptions = defaultRetryStrategyOptions,
                        bodyParser = dummyBodyParser,
                        listeners = errorPartialMockListener
                )
            }

            it("then the caller will be notified with the error") {
                argumentCaptor<Error>().apply {
                    verify(mockErrorCallback).invoke(capture())
                    assertThat(allValues[0]).isEqualTo(auth401Error)
                }
            }
        }

        describe("when subscribeNonResuming is called") {
            beforeEachTest {
                subject.subscribeNonResuming(
                        destination = dummyRequestDestination,
                        headers = emptyRequestHeaders,
                        tokenProvider = mockFailingTokenProvider,
                        tokenParams = null,
                        retryOptions = defaultRetryStrategyOptions,
                        bodyParser = dummyBodyParser,
                        listeners = errorPartialMockListener
                )
            }

            it("then the caller will be notified with the error") {
                argumentCaptor<Error>().apply {
                    verify(mockErrorCallback).invoke(capture())
                    assertThat(allValues[0]).isEqualTo(auth401Error)
                }
            }
        }

    }

})