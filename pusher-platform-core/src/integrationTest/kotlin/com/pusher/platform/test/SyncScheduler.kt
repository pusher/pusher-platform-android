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

class AsyncScheduler : MainThreadScheduler {
    override fun schedule(action: () -> Unit): ScheduledJob = schedule(0, action)

    override fun schedule(delay: Long, action: () -> Unit): ScheduledJob = ThreadJob(delay, action)
}

private class ThreadJob(val delay: Long, val action: () -> Unit) : Thread(), ScheduledJob {

    init {
        start()
    }

    override fun run() {
        if (delay > 0) sleep(delay)
        if (!isInterrupted) action()
    }

    override fun cancel() {
        interrupt()
    }

}
