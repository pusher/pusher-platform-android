package com.pusher.platform.tokenProvider

import com.pusher.platform.network.Promise
import com.pusher.util.Result
import elements.Error

interface TokenProvider {
    fun fetchToken(tokenParams: Any?): Promise<Result<String, Error>>
    fun clearToken(token: String? = null)
}
