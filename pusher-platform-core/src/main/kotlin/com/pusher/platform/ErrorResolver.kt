package com.pusher.platform

import com.pusher.platform.network.Futures
import com.pusher.platform.network.cancel
import com.pusher.platform.retrying.RetryStrategy
import com.pusher.platform.retrying.RetryStrategy.DoNotRetry
import com.pusher.platform.retrying.RetryStrategy.Retry
import com.pusher.platform.retrying.RetryStrategyOptions
import elements.Error
import elements.ErrorResponse
import elements.NetworkError
import elements.retryAfter
import java.lang.Thread.sleep
import java.util.*
import java.util.concurrent.Future
import kotlin.math.min

typealias RetryStrategyResultCallback = (RetryStrategy) -> Unit

fun String.isSafeRequest(): Boolean = when(this.toUpperCase()){
    "GET", "SUBSCRIBE", "HEAD", "PUT" -> true
    else -> false
}

fun ErrorResponse.isRetryable(): Boolean =
    this.statusCode in 500..599 &&
            this.headers["Request-Method"]?.firstOrNull()?.isSafeRequest() ?: false


class ErrorResolver(
    private val retryOptions: RetryStrategyOptions,
    private val retryUnsafeRequests: Boolean = false
) {

    private var currentRetryCount = 0
    private var currentBackoffMillis = 0L

    private val runningJobs = LinkedList<Future<*>>()

    fun resolveError(error: Error, callback: RetryStrategyResultCallback){
        when(error){
            is NetworkError -> {
                currentBackoffMillis = increaseCurrentBackoff()
                currentRetryCount += 1

                runningJobs += Futures.schedule {
                    sleep(currentBackoffMillis)
                    callback(Retry)
                }
            }
            is ErrorResponse -> {
                if(error.isRetryable() || retryUnsafeRequests){
                    if(retryOptions.limit < 0 || currentRetryCount <= retryOptions.limit){
                        currentBackoffMillis = error.headers.retryAfter.takeIf { it > 0 } ?: increaseCurrentBackoff()
                        currentRetryCount += 1

                        runningJobs += Futures.schedule {
                            sleep(currentBackoffMillis)
                            callback(Retry)
                        }
                    } else {
                        callback(DoNotRetry)
                    }
                } else {
                    callback(DoNotRetry)
                }
            }
            else -> callback(DoNotRetry)
        }
    }

    private fun increaseCurrentBackoff(): Long = when (currentRetryCount) {
        0 -> retryOptions.initialTimeoutMillis
        else -> min(retryOptions.maxTimeoutMillis, currentBackoffMillis * 2)
    }

    fun cancel() {
        while (runningJobs.isNotEmpty()) {
            runningJobs.pop().cancel()
        }
    }

}
