package com.pusher.platform.retrying

sealed class RetryStrategy {

    object Retry : RetryStrategy()
    object DoNotRetry : RetryStrategy()

}
