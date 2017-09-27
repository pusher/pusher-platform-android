package com.pusher.platform

import android.os.Handler
import com.pusher.platform.network.ConnectivityHelper
import com.pusher.platform.retrying.DoNotRetry
import com.pusher.platform.retrying.Retry
import com.pusher.platform.retrying.RetryStrategyOptions
import com.pusher.platform.retrying.RetryStrategyResult
import elements.Error
import elements.ErrorResponse
import elements.NetworkError

typealias RetryStrategyResultCallback = (RetryStrategyResult) -> Unit

fun String.isSafeRequest(): Boolean = when(this.toUpperCase()){
    "GET", "SUBSCRIBE", "HEAD", "PUT" -> true
    else -> false
}

fun ErrorResponse.isRetryable(): Boolean =
    this.statusCode in 500..599 &&
            this.headers["Request-Method"]?.firstOrNull()?.isSafeRequest() ?: false


class ErrorResolver(val connectivityHelper: ConnectivityHelper, val retryOptions: RetryStrategyOptions, val retryUnsafeRequests: Boolean = false) {

    var errorBeingResolved: Any = {}
    val handler = Handler()
    var retryNow: (() -> Unit)? = null

    var currentRetryCount = 0
    var currentBackoffMillis = 0L

    fun resolveError(error: Error, callback: RetryStrategyResultCallback){

        when(error){
            is NetworkError -> {
                retryNow = { callback(Retry())}
                connectivityHelper.onConnected(retryNow!!)
            }
            is ErrorResponse -> {

                //Retry-After present
                if (error.headers["Retry-After"] != null) {
                    val retryAfter = error.headers["Retry-After"]!![0].toLong() * 1000
                    retryNow = { callback(Retry())}
                    handler.postDelayed(retryNow, retryAfter)
                }

                else if(error.isRetryable() || retryUnsafeRequests){
                    if(retryOptions.limit < 0 || currentRetryCount <= retryOptions.limit ){
                        currentBackoffMillis = increaseCurrentBackoff()
                        currentRetryCount += 1

                        retryNow = { callback(Retry())}
                        handler.postDelayed(retryNow, currentBackoffMillis)
                    }

                    else{
                        callback(DoNotRetry())
                    }
                }

                else {
                    callback(DoNotRetry())
                }
            }
        }
    }

    private fun increaseCurrentBackoff(): Long {
        if(currentRetryCount == 0) return retryOptions.initialTimeoutMillis
        val newBackoff = currentBackoffMillis * 2

        if(newBackoff > retryOptions.maxTimeoutMillis) return retryOptions.maxTimeoutMillis
        else return newBackoff
    }

    fun cancel() {
        if(retryNow != null){
            handler.removeCallbacks(retryNow)
        }

    }

}