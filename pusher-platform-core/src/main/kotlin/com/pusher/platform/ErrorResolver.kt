package com.pusher.platform

import com.pusher.platform.network.Futures
import com.pusher.platform.network.cancel
import com.pusher.platform.network.flatMap
import com.pusher.platform.network.toFuture
import com.pusher.platform.retrying.RetryStrategy
import com.pusher.platform.retrying.RetryStrategy.DoNotRetry
import com.pusher.platform.retrying.RetryStrategy.Retry
import com.pusher.platform.retrying.RetryStrategyOptions
import elements.Error
import elements.ErrorResponse
import elements.NetworkError
import java.util.*
import java.util.concurrent.Future
import kotlin.math.min

fun String.isSafeRequest(): Boolean = when (this.toUpperCase()) {
    "GET", "SUBSCRIBE", "HEAD", "PUT" -> true
    else -> false
}

private val ErrorResponse.requestMethod
    get() = headers["Request-Method"]?.firstOrNull() ?: ""

class ErrorResolver(
    private val retryOptions: RetryStrategyOptions,
    private val retryUnsafeRequests: Boolean = false
) {

    private var currentRetryCount = 0
    private var currentBackoffMillis = 0L

    private val availableRetries
        get() = retryOptions.limit < 0 || currentRetryCount <= retryOptions.limit

    private val runningJobs = LinkedList<Future<*>>()

    fun resolveError(error: Error): Future<RetryStrategy> =
        error.retryStrategy().toFuture()
            .flatMap {
                error.takeIf { availableRetries }
                    ?.let { error.delayRetry() }
                    ?: DoNotRetry.toFuture<RetryStrategy>()
            }



    private fun Error.delayRetry(): Future<RetryStrategy> = Futures.schedule<RetryStrategy> {
        currentBackoffMillis = retryAfter.takeIf { it > 0 } ?: increaseCurrentBackoff()
        currentRetryCount += 1
        Thread.sleep(currentBackoffMillis)
        Retry
    }.also { runningJobs += it }

    private fun increaseCurrentBackoff(): Long = when (currentRetryCount) {
        0 -> retryOptions.initialTimeoutMillis
        else -> min(retryOptions.maxTimeoutMillis, currentBackoffMillis * 2)
    }

    fun cancel() {
        while (runningJobs.isNotEmpty()) {
            runningJobs.pop().cancel()
        }
    }

    private fun Error.retryStrategy(): RetryStrategy = when {
        retryUnsafeRequests -> Retry
        this is NetworkError -> Retry
        this is ErrorResponse -> retryStrategy()
        else -> DoNotRetry
    }

    private fun ErrorResponse.retryStrategy() = when {
        statusCode in 500..599 && requestMethod.isSafeRequest() -> Retry
        else -> DoNotRetry
    }

    private val Error.retryAfter: Long
        get() = let { it as? ErrorResponse }?.retryAfter ?: 0

}
