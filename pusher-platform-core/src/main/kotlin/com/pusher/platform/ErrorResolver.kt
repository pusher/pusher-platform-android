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
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.ArrayList
import kotlin.math.min
import kotlin.math.pow

/**
 * This class MUST BE THREADSAFE. It is primarily driven from the subscription thread, but will be
 * cancelled from outside.
 */
internal class ErrorResolver(
    private val retryOptions: RetryStrategyOptions,
    private val retryUnsafeRequests: Boolean = false
) {
    private val runningJobs = ConcurrentLinkedQueue<Future<*>>()
    private val currentRetryCount = AtomicInteger(0)
    private val initialBackOff = retryOptions.initialTimeoutMillis.toDouble()
    private val currentBackoffMillis: Long
        get() = min(
            initialBackOff.pow(currentRetryCount.get()).toLong(),
            retryOptions.maxTimeoutMillis
        )

    private val noRetriesRemaining: Boolean
        get() = min(0, retryOptions.limit - currentRetryCount.get()) == 0

    fun resolveError(error: Error, block: (RetryStrategy) -> Unit) = when (strategyFor(error)) {
        is DoNotRetry -> block(DoNotRetry)
        is Retry -> runningJobs += defer(error.retryAfter, block)
    }

    fun cancel() {
        runningJobs.forEach { it.cancel() }
    }

    private fun defer(retryAfter: Long, block: (RetryStrategy) -> Unit): Future<Unit> = Futures.schedule {
        currentRetryCount.incrementAndGet()
        Thread.sleep(if (retryAfter > 0) retryAfter else currentBackoffMillis)
        block(Retry)
    }

    private fun strategyFor(error: Error): RetryStrategy = when {
        retryUnsafeRequests -> Retry
        noRetriesRemaining -> DoNotRetry
        error is NetworkError -> Retry
        error is ErrorResponse -> error.errorResponseStrategy()
        else -> DoNotRetry
    }

}

/**
 * Header `Retry-After` or 0 if missing
 */
private val Error.retryAfter: Long
    get() = let { it as? ErrorResponse }?.retryAfter ?: 0

/**
 * True if the request is safe to retry
 */
private fun String.isSafeRequest(): Boolean = when (this.toUpperCase()) {
    "GET", "SUBSCRIBE", "HEAD", "PUT" -> true
    else -> false
}

/**
 * Only retry if the error is over 500 and it is safe to retry
 */
private fun ErrorResponse.errorResponseStrategy() = when {
    statusCode in 500..599 && requestMethod.isSafeRequest() -> Retry
    else -> DoNotRetry
}

private val ErrorResponse.requestMethod
    get() = headers["Request-Method"]?.firstOrNull() ?: ""
