package com.pusher.platform.subscription

import java.util.concurrent.atomic.AtomicInteger

object SubscriptionIDGenerator {
    private val nextID = AtomicInteger(0)

    fun next() = "%04d".format(nextID.getAndIncrement())
}