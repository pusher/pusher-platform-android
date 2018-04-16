package com.pusher.platform.test

import com.pusher.platform.MainThreadScheduler
import com.pusher.platform.ScheduledJob

class SyncScheduler : MainThreadScheduler {
    override fun schedule(action: () -> Unit): ScheduledJob {
        action()
        return object : ScheduledJob {
            override fun cancel() {}
        }
    }

    override fun schedule(delay: Long, action: () -> Unit): ScheduledJob = schedule(action)
}
