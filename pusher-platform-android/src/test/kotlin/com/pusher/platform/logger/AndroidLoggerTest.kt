package com.pusher.platform.logger

import android.util.LogCapture
import android.util.LogSpy
import com.google.common.truth.Truth.assertThat
import com.pusher.platform.logger.LogLevel.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

private const val EXPECTED_TAG = "pusherPlatform"
private const val EXPECTED_MESSAGE = "message"
private val EXPECTED_ERROR = Error("Awesome error")

class AndroidLoggerTest {

    @ParameterizedTest(name = "should log {0}")
    @EnumSource(LogLevel::class)
    fun shouldLog(logLevel: LogLevel) {
        val doLog: Logger.(String, Error?) -> Unit = logLevel.doLog
        val log = LogSpy()
        val logger = AndroidLogger(VERBOSE)

        logger.doLog(EXPECTED_MESSAGE, null)

        assertThat(log.captures)
            .containsExactly(captureOf(logLevel))
    }


    @ParameterizedTest(name = "should log {0} with Error")
    @EnumSource(LogLevel::class)
    fun shouldLog_withError(logLevel: LogLevel) {
        val doLog: Logger.(String, Error?) -> Unit = logLevel.doLog
        val log = LogSpy()
        val logger = AndroidLogger(VERBOSE)

        logger.doLog(EXPECTED_MESSAGE, EXPECTED_ERROR)

        assertThat(log.captures)
            .containsExactly(captureOf(logLevel, error = EXPECTED_ERROR))
    }

    @Nested
    class Threshold {

        @ParameterizedTest(name = "should log {0} when threshold is higher")
        @EnumSource(LogLevel::class)
        fun shouldNotLog_whenThresholdIsHigher(logLevel: LogLevel) {
            val log = LogSpy()
            val logger = AndroidLogger(logLevel)

            logger.verbose(EXPECTED_MESSAGE)
            logger.debug(EXPECTED_MESSAGE)
            logger.info(EXPECTED_MESSAGE)
            logger.warn(EXPECTED_MESSAGE)
            logger.error(EXPECTED_MESSAGE)

            assertThat(log.captures).containsExactlyElementsIn(logLevel.expectedCaptures)
        }

    }

}

private val LogLevel.expectedCaptures
    get() = when (this) {
        LogLevel.VERBOSE -> capturesOf(VERBOSE, DEBUG, INFO, WARN, ERROR)
        LogLevel.DEBUG -> capturesOf(DEBUG, INFO, WARN, ERROR)
        LogLevel.INFO -> capturesOf(INFO, WARN, ERROR)
        LogLevel.WARN -> capturesOf(WARN, ERROR)
        LogLevel.ERROR -> capturesOf(ERROR)
    }

private val LogLevel.doLog: Logger.(String, Error?) -> Unit
    get() = when (this) {
        LogLevel.VERBOSE -> Logger::verbose
        LogLevel.DEBUG -> Logger::debug
        LogLevel.INFO -> Logger::info
        LogLevel.WARN -> Logger::warn
        LogLevel.ERROR -> Logger::error
    }

fun captureOf(
    logLevel: LogLevel,
    message: String = EXPECTED_MESSAGE,
    error: Error? = null
) = LogCapture(logLevel, EXPECTED_TAG, message, error)

fun capturesOf(vararg logLevels: LogLevel): List<LogCapture> = logLevels.map { captureOf(it) }
