package com.pusher.platform.logger

import android.util.LogCapture
import android.util.LogSpy
import com.google.common.truth.Truth.assertThat
import com.pusher.platform.logger.LogLevel.*
import org.junit.jupiter.api.Test
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
            arrayOf(level, when (level) {
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
        val log = LogSpy()
        val logger = AndroidLogger(VERBOSE)

        logger.doLog(EXPECTED_MESSAGE, null)

        assertThat(log.captures)
            .containsExactly(captureOf(logLevel))
    }

    @Test
    fun shouldLog_withError() {
        val log = LogSpy()
        val logger = AndroidLogger(VERBOSE)

        logger.doLog(EXPECTED_MESSAGE, EXPECTED_ERROR)

        assertThat(log.captures)
            .containsExactly(captureOf(logLevel, error = EXPECTED_ERROR))
    }

    @RunWith(Parameterized::class)
    class Threshold(
        private val logLevelThreshold: LogLevel,
        private val expectedCaptures: Iterable<LogCapture>
    ) {

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun data() = arrayOf(
                arrayOf(VERBOSE, capturesOf(VERBOSE, DEBUG, INFO, WARN, ERROR)),
                arrayOf(DEBUG, capturesOf(DEBUG, INFO, WARN, ERROR)),
                arrayOf(INFO, capturesOf(INFO, WARN, ERROR)),
                arrayOf(WARN, capturesOf(WARN, ERROR)),
                arrayOf(ERROR, capturesOf(ERROR))
            )
        }

        @Test
        fun shouldNotLog_whenThresholdIsHigher() {
            val log = LogSpy()
            val logger = AndroidLogger(logLevelThreshold)

            logger.verbose(EXPECTED_MESSAGE)
            logger.debug(EXPECTED_MESSAGE)
            logger.info(EXPECTED_MESSAGE)
            logger.warn(EXPECTED_MESSAGE)
            logger.error(EXPECTED_MESSAGE)

            assertThat(log.captures).containsExactlyElementsIn(expectedCaptures)
        }

    }

}

fun captureOf(
    logLevel: LogLevel,
    message: String = EXPECTED_MESSAGE,
    error: Error? = null
) = LogCapture(logLevel, EXPECTED_TAG, message, error)

fun capturesOf(vararg logLevels: LogLevel): List<LogCapture> = logLevels.map { captureOf(it) }
