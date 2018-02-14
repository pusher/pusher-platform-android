package com.pusher.platform.retrying

data class RetryStrategyOptions(val initialTimeoutMillis: Long = 1000L, val maxTimeoutMillis: Long = 5000L, val limit: Int = -1)