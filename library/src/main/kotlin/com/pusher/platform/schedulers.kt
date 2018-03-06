package com.pusher.platform

import android.os.Handler
import android.os.Looper
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

typealias Action = () -> Unit

class BackgroundScheduler(
    threadLimit: Int = 50,
    theadAliveTime: Long = 60
) : Scheduler {

    private val workQueue = LinkedBlockingQueue<Runnable>()
    private val pool = ThreadPoolExecutor(
        Runtime.getRuntime().availableProcessors(),
        threadLimit,
        theadAliveTime,
        TimeUnit.SECONDS,
        workQueue
    )

    override fun schedule(action: () -> Unit): ScheduledJob =
        CancellableScheduledJob(
            doAction = action,
            doPost = { pool.execute(task) },
            doCancel = { pool.remove(task) }
        )

    override fun schedule(delay: Long, action: () -> Unit): ScheduledJob =
        CancellableScheduledJob(
            doAction = action,
            doPost = {
                pool.execute {
                    Thread.sleep(delay)
                    schedule(task)
                }
            },
            doCancel = { pool.remove(task) }
        )

}

class ForegroundScheduler : MainThreadScheduler {

    private val handler = Handler(Looper.getMainLooper())

    override fun schedule(action: () -> Unit): ScheduledJob =
        CancellableScheduledJob(
            doAction = action,
            doPost = { handler.post(task) },
            doCancel = { handler.removeCallbacks(task) }
        )

    override fun schedule(delay: Long, action: () -> Unit): ScheduledJob =
        CancellableScheduledJob(
            doAction = action,
            doPost = { handler.postDelayed(task, delay) },
            doCancel = { handler.removeCallbacksAndMessages(task) }
        )
}

private class CancellableScheduledJob(
    doAction: Action,
    doPost: CancellableScheduledJob.() -> Unit,
    private val doCancel: CancellableScheduledJob.() -> Unit
) : ScheduledJob {

    private var active = true
    val task = {
        if (active) {
            doAction()
        }
    }

    init {
        doPost()
    }

    override fun cancel() {
        active = false
        doCancel()
    }

}
