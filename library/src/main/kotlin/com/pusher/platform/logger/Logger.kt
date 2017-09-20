package com.pusher.platform.logger

interface Logger {

    fun verbose(message: String, error: Error?)
    fun debug(message: String, error: Error?)
    fun info(message: String, error: Error?)
    fun warn(message: String, error: Error?)
    fun error(message: String, error: Error?)
}





