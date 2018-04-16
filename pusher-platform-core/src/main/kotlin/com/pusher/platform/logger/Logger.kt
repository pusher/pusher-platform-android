package com.pusher.platform.logger

interface Logger {
    fun verbose(message: String, error: Error? = null)
    fun debug(message: String, error: Error? = null)
    fun info(message: String, error: Error? = null)
    fun warn(message: String, error: Error? = null)
    fun error(message: String, error: Error? = null)
}





