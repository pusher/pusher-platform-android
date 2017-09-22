package com.pusher.platform.retrying

data class RetryStrategyOptions(val initialTimeoutMillis: Int = 1000, val maxTimeoutMillis: Int = 5000, val limit: Int = -1)