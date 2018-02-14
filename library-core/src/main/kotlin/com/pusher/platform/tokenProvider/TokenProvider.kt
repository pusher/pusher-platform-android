package com.pusher.platform.tokenProvider

import com.pusher.platform.Cancelable
import elements.Error

interface TokenProvider {
    fun fetchToken(tokenParams: Any? = null, onSuccess: (String) -> Unit, onFailure: (Error) -> Unit): Cancelable
    fun clearToken(token: String? = null)
}
