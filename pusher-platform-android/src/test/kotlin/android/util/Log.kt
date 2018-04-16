@file:Suppress("unused")

package android.util

import com.pusher.platform.logger.LogLevel

val spies = mutableListOf<LogSpy>()

/**
 * Synthetic double for [android.util.Log]. Classloader will use this instead of the platform one.
 */
class Log {

    companion object {

        const val VERBOSE = 2
        const val DEBUG = 3
        const val INFO = 4
        const val WARN = 5
        const val ERROR = 6
        const val ASSERT = 7

        fun spy() = LogSpy()

        private fun record(capture: LogCapture): Int {
            spies.forEach { it.record(capture) }
            return 0
        }

        @JvmStatic @JvmOverloads
        fun v(tag: String, msg: String, tr: Throwable? = null): Int = record(LogCapture(LogLevel.VERBOSE, tag, msg, tr))

        @JvmStatic @JvmOverloads
        fun d(tag: String, msg: String, tr: Throwable? = null): Int = record(LogCapture(LogLevel.DEBUG, tag, msg, tr))

        @JvmStatic @JvmOverloads
        fun i(tag: String, msg: String, tr: Throwable? = null): Int = record(LogCapture(LogLevel.INFO, tag, msg, tr))

        @JvmStatic @JvmOverloads
        fun w(tag: String, msg: String = "", tr: Throwable? = null): Int = record(LogCapture(LogLevel.WARN, tag, msg, tr))

        @JvmStatic @JvmOverloads
        fun e(tag: String, msg: String, tr: Throwable? = null): Int = record(LogCapture(LogLevel.ERROR, tag, msg, tr))

    }

}

data class LogCapture(val logLevel: LogLevel, val tag: String, val message: String, val error: Throwable? = null)

class LogSpy {

    private val _captures = mutableListOf<LogCapture>()
    val captures: List<LogCapture> = _captures

    init {
        spies += this
    }

    fun record(capture: LogCapture) {
        _captures += capture
    }

}