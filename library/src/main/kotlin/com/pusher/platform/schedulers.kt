package com.pusher.platform

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper

typealias Action = () -> Unit

sealed class AndroidScheduler(private val handler: Handler) : Scheduler {

    override fun schedule(action: Action): ScheduledJob =
        HandlerScheduledJob(handler, action) { post(it) }

    override fun schedule(delay: Long, action: () -> Unit): ScheduledJob =
        HandlerScheduledJob(handler, action) { postDelayed(it, delay) }

}

private fun createBackgroundHandler(name: String): Handler {
    val handlerThread = HandlerThread(name).apply { start() }
    return Handler(handlerThread.looper)
}

class BackgroundScheduler(name: String) : AndroidScheduler(createBackgroundHandler(name))

class ForegroundScheduler : AndroidScheduler(Handler(Looper.getMainLooper())), MainThreadScheduler

class HandlerScheduledJob(
    private val handler: Handler,
    private val action: Action,
    doPost: Handler.(Action) -> Unit
) : ScheduledJob {

    init {
        handler.doPost(action)
    }

    override fun cancel() = handler.removeCallbacks(action)

}

