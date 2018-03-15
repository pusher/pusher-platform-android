package com.pusher.platform.network

import com.pusher.util.Result
import elements.Error
import okhttp3.Response

typealias OkHttpResponsePromise = Promise<Result<Response, Error>>
