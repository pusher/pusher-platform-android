package com.pusher.platform.logger

import com.google.common.truth.Truth.assertThat
import com.pusher.platform.logger.LogCapture.*
import com.pusher.platform.logger.LogLevel.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private const val EXPECTED_TAG = "pusherPlatform"
private const val EXPECTED_MESSAGE = "message"

@RunWith(Parameterized::class)
class AndroidLoggerAndroidTest(
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
        val logger = AndroidLogger(VERBOSE)
        val logs = LogCaptor()

        logger.doLog(EXPECTED_MESSAGE, null)

        assertThat(logs.withTag(EXPECTED_TAG))
            .contains(Message(logLevel, EXPECTED_TAG, "message"))
    }

    @Test
    fun shouldLog_withError() {
        val logger = AndroidLogger(VERBOSE)
        val logs = LogCaptor()

        logger.doLog(EXPECTED_MESSAGE, Error("Awesome error"))

        assertThat(logs.withTag(EXPECTED_TAG))
            .contains(Message(logLevel, EXPECTED_TAG, "message"))
    }



}
