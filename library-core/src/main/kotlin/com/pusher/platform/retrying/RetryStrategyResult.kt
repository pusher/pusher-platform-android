package com.pusher.platform.retrying

sealed class RetryStrategyResult

class Retry: RetryStrategyResult()

class DoNotRetry: RetryStrategyResult()