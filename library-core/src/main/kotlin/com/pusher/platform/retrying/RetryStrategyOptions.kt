package com.pusher.platform.retrying

/**
 * Describes options to be followed by the SDK when retrying failed requests.
 */
data class RetryStrategyOptions(
    val initialTimeoutMillis: Long = 1000L,
    val maxTimeoutMillis: Long = 5000L,
    val limit: Int = -1
)
