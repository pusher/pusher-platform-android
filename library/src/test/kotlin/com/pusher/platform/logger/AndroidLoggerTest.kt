package com.pusher.platform.logger

import android.util.LogCapture
import android.util.LogSpy
import com.google.common.truth.Truth.assertThat
import com.pusher.platform.logger.LogLevel.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private const val EXPECTED_TAG = "pusherPlatform"
private const val EXPECTED_MESSAGE = "message"
private val EXPECTED_ERROR = Error("Awesome error")

@RunWith(Parameterized::class)
class AndroidLoggerTest(
    private val logLevel: LogLevel,
    private val doLog: Logger.(String, Error?) -> Unit
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = values().map { level ->
            arrayOf(level, when(level) {
                VERBOSE -> Logger::verbose
                DEBUG -> Logger::debug
                INFO -> Logger::info
                WARN -> Logger::warn
                ERROR -> Logger::error
            })
        }
    }

    @Test
    fun shouldLog() {
        val logSpy = LogSpy()
        val logger = AndroidLogger(VERBOSE)

        logger.doLog(EXPECTED_MESSAGE, null)

        assertThat(logSpy.captures)
            .contains(LogCapture(logLevel, EXPECTED_TAG, EXPECTED_MESSAGE))
    }

    @Test
    fun shouldLog_withError() {
        val logSpy = LogSpy()
        val logger = AndroidLogger(VERBOSE)

        logger.doLog(EXPECTED_MESSAGE, EXPECTED_ERROR)

        assertThat(logSpy.captures)
            .contains(LogCapture(logLevel, EXPECTED_TAG, EXPECTED_MESSAGE, EXPECTED_ERROR))
    }



}
