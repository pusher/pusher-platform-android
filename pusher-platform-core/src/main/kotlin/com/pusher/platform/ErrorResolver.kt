package com.pusher.platform

import com.pusher.platform.network.ConnectivityHelper
import com.pusher.platform.retrying.DoNotRetry
import com.pusher.platform.retrying.Retry
import com.pusher.platform.retrying.RetryStrategyOptions
import com.pusher.platform.retrying.RetryStrategyResult
import elements.Error
import elements.ErrorResponse
import elements.NetworkError
import elements.retryAfter
import java.util.*

typealias RetryStrategyResultCallback = (RetryStrategyResult) -> Unit

fun String.isSafeRequest(): Boolean = when(this.toUpperCase()){
    "GET", "SUBSCRIBE", "HEAD", "PUT" -> true
    else -> false
}

fun ErrorResponse.isRetryable(): Boolean =
    this.statusCode in 500..599 &&
            this.headers["Request-Method"]?.firstOrNull()?.isSafeRequest() ?: false


class ErrorResolver(
    private val connectivityHelper: ConnectivityHelper,
    private val retryOptions: RetryStrategyOptions,
    private val scheduler: Scheduler,
    private val retryUnsafeRequests: Boolean = false
) {

    private var currentRetryCount = 0
    private var currentBackoffMillis = 0L

    private val runningJobs = LinkedList<ScheduledJob>()

    fun resolveError(error: Error, callback: RetryStrategyResultCallback){
        when(error){
            is NetworkError -> {
                connectivityHelper.onConnected { callback(Retry())}
            }
            is ErrorResponse -> {
                if(error.isRetryable() || retryUnsafeRequests){
                    if(retryOptions.limit < 0 || currentRetryCount <= retryOptions.limit){
                        currentBackoffMillis = error.headers.retryAfter.takeIf { it > 0 } ?: increaseCurrentBackoff()
                        currentRetryCount += 1

                        runningJobs += scheduler.schedule(currentBackoffMillis) { callback(Retry())}
                    } else{
                        callback(DoNotRetry())
                    }
                } else {
                    callback(DoNotRetry())
                }
            }
            else -> callback(DoNotRetry())
        }
    }

    private fun increaseCurrentBackoff(): Long {
        if(currentRetryCount == 0) return retryOptions.initialTimeoutMillis
        val newBackoff = currentBackoffMillis * 2

        if(newBackoff > retryOptions.maxTimeoutMillis) return retryOptions.maxTimeoutMillis
        else return newBackoff
    }

    fun cancel() {
        while (runningJobs.isNotEmpty()) {
            runningJobs.pop().cancel()
        }
    }

}
