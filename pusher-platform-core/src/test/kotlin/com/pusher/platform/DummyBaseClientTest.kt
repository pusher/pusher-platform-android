package com.pusher.platform

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.*
import com.pusher.platform.network.DataParser
import com.pusher.platform.network.Futures
import com.pusher.platform.retrying.RetryStrategyOptions
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.asFailure
import com.pusher.util.asSuccess
import elements.Error
import elements.Errors
import elements.emptyHeaders
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.RETURNS_SMART_NULLS

internal class DummyBaseClientTest {

    lateinit var subject: BaseClient

    @Before
    fun setUpSubject() {
        subject = BaseClient("dummy", TestDependencies())
    }

    @Test
    fun `notifies error on token fetch error when subscribeResuming is called`() {
        val dummyRequestDestination = RequestDestination.Relative("dummy") // irrelevant here
        val emptyRequestHeaders = emptyHeaders() // also irrelevant for this test
        val authExpired401Error = Errors.response(401, emptyHeaders(), "test error body")
        val mock401TokenProvider = mockFailingTokenProvider(authExpired401Error)
        val mockErrorCallback = mock<(Error) -> Unit>()
        val defaultRetryStrategyOptions = RetryStrategyOptions() // effectively no retrying
        val dummyBodyParser: DataParser<String> = { "".asSuccess() } // irrelevant for this test
        val errorPartialMockListener = errorPartialMockListener(mockErrorCallback)

        subject.subscribeResuming(
                destination = dummyRequestDestination,
                headers = emptyRequestHeaders,
                tokenProvider = mock401TokenProvider,
                tokenParams = null,
                retryOptions = defaultRetryStrategyOptions,
                bodyParser = dummyBodyParser,
                listeners = errorPartialMockListener
        )

        argumentCaptor<Error>().apply {
            verify(mockErrorCallback).invoke(capture())
            assertThat(allValues[0]).isEqualTo(authExpired401Error)
        }
    }

    @Test
    fun `notifies error on token fetch error when subscribeNonResuming is called`() {
        val dummyRequestDestination = RequestDestination.Relative("dummy") // irrelevant here
        val emptyRequestHeaders = emptyHeaders() // also irrelevant for this test
        val authExpired401Error = Errors.response(401, emptyHeaders(), "test error body")
        val mock401TokenProvider = mockFailingTokenProvider(authExpired401Error)
        val mockErrorCallback = mock<(Error) -> Unit>()
        val defaultRetryStrategyOptions = RetryStrategyOptions() // effectively no retrying
        val dummyBodyParser: DataParser<String> = { "".asSuccess() } // irrelevant for this test
        val errorPartialMockListener = errorPartialMockListener(mockErrorCallback)

        subject.subscribeNonResuming(
                destination = dummyRequestDestination,
                headers = emptyRequestHeaders,
                tokenProvider = mock401TokenProvider,
                tokenParams = null,
                retryOptions = defaultRetryStrategyOptions,
                bodyParser = dummyBodyParser,
                listeners = errorPartialMockListener
        )

        argumentCaptor<Error>().apply {
            verify(mockErrorCallback).invoke(capture())
            assertThat(allValues[0]).isEqualTo(authExpired401Error)
        }
    }

    private fun mockFailingTokenProvider(authExpired401Error: Error): TokenProvider = mock {
        on { fetchToken(anyOrNull()) } doReturn failureAs(authExpired401Error)
    }


    private fun failureAs(authExpired401Error: Error) = Futures.schedule {
        authExpired401Error.asFailure<String, Error>()
    }

    private fun errorPartialMockListener(mockErrorCallback: (Error) -> Unit):
            SubscriptionListeners<String> = mock(defaultAnswer = RETURNS_SMART_NULLS) {
        on { onError } doReturn (mockErrorCallback)
    }

}