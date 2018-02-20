package com.pusher.platform.logger

import android.util.Log
import com.pusher.platform.logger.AndroidLogger.LogStrategy.*
import com.pusher.platform.logger.LogLevel.*

class AndroidLogger(val threshold: LogLevel) : Logger {

    private val tag = "pusherPlatform"

    override fun verbose(message: String, error: Error?) =
        VERBOSE.strategy.log(tag, message, error)

    override fun debug(message: String, error: Error?) =
        DEBUG.strategy.log(tag, message, error)

    override fun info(message: String, error: Error?) =
        INFO.strategy.log(tag, message, error)

    override fun warn(message: String, error: Error?) =
        WARN.strategy.log(tag, message, error)

    override fun error(message: String, error: Error?) =
        ERROR.strategy.log(tag, message, error)

    private val LogLevel.strategy
        get() = if (this >= threshold) {
            when (this) {
                VERBOSE -> Verbose
                DEBUG -> Debug
                INFO -> Info
                WARN -> Warning
                ERROR -> Err
            }
        } else {
            Disabled
        }

    private sealed class LogStrategy(
        private val onLog: (String, String) -> Unit,
        private val onLogWithError: (String, String, Error) -> Unit
    ) {

        fun log(tag: String, message: String, error: Error?) {
            if (error != null) onLogWithError(tag, message, error)
            else onLog(tag, message)
        }

        object Disabled : LogStrategy(
            { _, _ -> },
            { _, _, _ -> }
        )

        object Verbose : LogStrategy(
            { tag, message -> Log.v(tag, message) },
            { tag, message, error -> Log.v(tag, message, error) }
        )

        object Debug : LogStrategy(
            { tag, message -> Log.d(tag, message) },
            { tag, message, error -> Log.d(tag, message, error) }
        )

        object Info : LogStrategy(
            { tag, message -> Log.i(tag, message) },
            { tag, message, error -> Log.i(tag, message, error) }
        )

        object Warning : LogStrategy(
            { tag, message -> Log.w(tag, message) },
            { tag, message, error -> Log.w(tag, message, error) }
        )

        object Err : LogStrategy(
            { tag, message -> Log.d(tag, message) },
            { tag, message, error -> Log.d(tag, message, error) }
        )

    }
}