package com.pusher.util

import com.nhaarman.mockito_kotlin.anyOrNull
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.pusher.platform.network.Futures
import com.pusher.platform.tokenProvider.TokenProvider
import elements.Error

fun mockFailingTokenProvider(error: Error): TokenProvider = mock {
    on { fetchToken(anyOrNull()) } doReturn failureAs(error)
}

private fun failureAs(error: Error) = Futures.schedule {
    error.asFailure<String, Error>()
}