package com.pusher.platform.tokenProvider

import com.pusher.platform.CancelableRequest

interface TokenProvider {
    fun fetchToken(tokenParams: Any?): CancelableRequest
    fun clearToken(token: String?)
}