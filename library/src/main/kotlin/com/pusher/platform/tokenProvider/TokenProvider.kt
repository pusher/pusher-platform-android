package com.pusher.platform.tokenProvider

import com.pusher.platform.Cancelable

interface TokenProvider {
    fun fetchToken(tokenParams: Any?): Cancelable
    fun clearToken(token: String?)
}