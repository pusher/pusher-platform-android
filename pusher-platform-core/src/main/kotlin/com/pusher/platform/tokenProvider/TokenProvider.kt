package com.pusher.platform.tokenProvider

import com.pusher.util.Result
import elements.Error
import java.util.concurrent.Future

interface TokenProvider {
    fun fetchToken(tokenParams: Any?): Future<Result<String, Error>>
    fun clearToken(token: String? = null)
}
