package com.pusher.platform.logger

import android.util.Log

class AndroidLogger(val threshold: LogLevel): Logger {

    val tag = "pusherPlatform"

    override fun verbose(message: String, error: Error?) {
        log(logLevel = LogLevel.VERBOSE, message = message, error = error)
    }

    override fun debug(message: String, error: Error?) {
        log(logLevel = LogLevel.DEBUG, message = message, error = error)
    }

    override fun info(message: String, error: Error?) {
        log(logLevel = LogLevel.INFO, message = message, error = error)
    }

    override fun warn(message: String, error: Error?) {
        log(logLevel = LogLevel.WARN, message = message, error = error)
    }

    override fun error(message: String, error: Error?) {
        log(logLevel = LogLevel.ERROR, message = message, error = error)
    }

    private fun log(logLevel: LogLevel, message: String, error: Error?){
        if(logLevel >= threshold){

            when(logLevel){
                LogLevel.VERBOSE -> {
                    if (error != null) {
                        Log.v(tag, message, error)
                    }
                    else {
                        Log.v(tag, message)
                    }
                }
                LogLevel.DEBUG -> {
                    if (error != null) {
                        Log.d(tag, message, error)
                    }
                    else {
                        Log.d(tag, message)
                    }
                }
                LogLevel.INFO -> {
                    if (error != null) {
                        Log.i(tag, message, error)
                    }
                    else {
                        Log.i(tag, message)
                    }
                }
                LogLevel.WARN -> {
                    if (error != null) {
                        Log.w(tag, message, error)
                    }
                    else {
                        Log.w(tag, message)
                    }
                }
                LogLevel.ERROR -> {
                    if (error != null) {
                        Log.e(tag, message, error)
                    }
                    else {
                        Log.e(tag, message)
                    }
                }
            }
        }
    }
}