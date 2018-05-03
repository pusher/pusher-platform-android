package com.pusher.platform.logger

interface Logger {
    fun verbose(message: String, error: Error? = null)
    fun debug(message: String, error: Error? = null)
    fun info(message: String, error: Error? = null)
    fun warn(message: String, error: Error? = null)
    fun error(message: String, error: Error? = null)
}

/**
 * Allows for fluent logging
 */
fun <A> Logger.log(subject: A, block: Logger.(A) -> Unit) : A =
    subject.logWith(this, block)

/**
 * Same as [log] but for intermediate function chains
 */
fun <A> A.logWith(logger: Logger, block: Logger.(A) -> Unit) : A {
    logger.block(this)
    return this
}
