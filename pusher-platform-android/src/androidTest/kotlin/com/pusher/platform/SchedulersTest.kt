package com.pusher.platform

import android.os.Looper
import com.google.common.truth.Truth.assertThat
import org.junit.Test

private const val DELAY = 20L

class SchedulersTest {

    @Test
    fun mainThreadScheduler_runsOnMainLooper() {
        val scheduler = ForegroundScheduler()
        var looper by FutureValue<Looper?>()

        scheduler.schedule { looper = Looper.myLooper() }

        assertThat(looper).isEqualTo(Looper.getMainLooper())
    }

    @Test
    fun backgroundScheduler_runsOnBackground() {
        val scheduler = BackgroundScheduler()
        var looper by FutureValue<Looper?>()

        scheduler.schedule { looper = Looper.myLooper() }

        assertThat(looper).isNotEqualTo(Looper.getMainLooper())
    }

    @Test
    fun scheduler_runsDelayed() {
        val start = System.currentTimeMillis()
        val scheduler = BackgroundScheduler()
        var result by FutureValue<Long>()

        scheduler.schedule(DELAY) { result = System.currentTimeMillis() - start }

        assertThat(result).isAtLeast(DELAY)
    }

    @Test
    fun scheduler_allowsCancellation() {
        val scheduler = BackgroundScheduler()
        var result by FutureValue<Boolean>()

        val job = scheduler.schedule(DELAY) { result = false }
        scheduler.schedule(DELAY) { result = true }
        job.cancel()

        assertThat(result).isTrue()
    }

}
