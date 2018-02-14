package com.pusher.platform

interface MainThreadScheduler : Scheduler

interface Scheduler {

    fun schedule(action: () -> Unit) : ScheduledJob
    fun schedule(delay: Long, action: () -> Unit) : ScheduledJob

}

interface ScheduledJob {
    fun cancel()
}

