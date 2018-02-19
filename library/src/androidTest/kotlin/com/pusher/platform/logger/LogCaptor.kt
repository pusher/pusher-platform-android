package com.pusher.platform.logger

import com.pusher.platform.logger.LogLevel.*
import org.junit.rules.ExternalResource

sealed class LogCapture {
    companion object {
        operator fun invoke(input: String): LogCapture = when (input) {
            "--------- beginning of main" -> LogStart
            else -> resolveMessage(input)
        }

        private fun resolveMessage(input: String) = try {
            val (meta, message) = input.split(": ")
            val (level, tag) = meta.split(" ").filter { it.isNotBlank() }.drop(4)
            LogCapture.Message(level.asLogLevel(), tag, message)
        } catch (e: Exception) {
            LogCapture.Invalid(input, e)
        }

        private fun String.asLogLevel(): LogLevel = when (this) {
            "V" -> VERBOSE
            "D" -> DEBUG
            "I" -> INFO
            "W" -> WARN
            "E" -> ERROR
            else -> throw IllegalArgumentException("Unknown log level")
        }
    }

    object LogStart : LogCapture()
    data class Message(val logLevel: LogLevel, val tag: String, val message: String) : LogCapture()
    data class Invalid(val message: String, val e: Throwable) : LogCapture()
}

class LogCaptor : ExternalResource() {

    val input = ProcessBuilder()
        .command("logcat").start()
        .inputStream.bufferedReader()
        .lineSequence()
        .map { LogCapture(it) }
        .filterNot { it == LogCapture.LogStart }


    fun withTag(tag: String) = input.filter { it is LogCapture.Message && it.tag == tag }.asIterable()
}