package com.pusher.platform.network

import java.util.concurrent.*
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicInteger

private val threadCount = AtomicInteger()
private val futuresExecutorService: ExecutorService = Executors.newCachedThreadPool{
    Thread(it, "Pusher-Thread-${threadCount.getAndIncrement()}")
}

object Futures {

    @JvmStatic
    fun <V> schedule(service: ExecutorService = futuresExecutorService, block: () -> V): Future<V> =
        service.submit(block)

    @JvmStatic
    fun <V> now(value: V): Future<V> =
        schedule { value }

    @JvmStatic
    fun <V, R> map(original: Future<V>, block: (V) -> R) : Future<R> =
        MapFuture(original, block)

    @JvmStatic
    fun <V, R> flatMap(original: Future<V>, block: (V) -> Future<R>) : Future<R> =
        FlatMapFuture(original, block)

}

/**
 * Same as [Futures.now]
 */
fun <A> A.toFuture(): Future<A> =
    Futures.now(this)

/**
 * Derives a new [Future] using the provided transformation.
 */
fun <V, R> Future<V>.map(block: (V) -> R) =
    Futures.map(this, block)

/**
 * Derives a new [Future] from the provided transformation.
 */
fun <V, R> Future<V>.flatMap(block: (V) -> Future<R>) =
    Futures.flatMap(this, block)

/**
 * Short hand for `cancel(true)`
 */
fun <V> Future<V>.cancel() = cancel(true)

/**
 * Internal implementation to map from one future to a new one used by [map]
 */
private class MapFuture<V, R>(val future: Future<V>, val block: (V) -> R) : Future<R> {
    override fun isDone() = future.isDone
    override fun get(): R = future.get().let(block)
    override fun get(time: Long, unit: TimeUnit?): R = future.get(time, unit).let(block)
    override fun cancel(mayInterruptIfRunning: Boolean) = future.cancel(mayInterruptIfRunning)
    override fun isCancelled() = future.isCancelled
}

/**
 * Internal implementation to map from one future to a new one used by [flatMap]
 */
private class FlatMapFuture<V, R>(val future: Future<V>, val block: (V) -> Future<R>) : Future<R> {
    override fun isDone() = future.isDone
    override fun get(): R = future.get().let(block).get()
    override fun get(time: Long, unit: TimeUnit?): R = future.get(time, unit).let(block).get(time, unit)
    override fun cancel(mayInterruptIfRunning: Boolean) = future.cancel(mayInterruptIfRunning)
    override fun isCancelled() = future.isCancelled
}

/**
 * Adds the option to wait blocking for a future to be ready with a default wait of 10 seconds
 */
fun <V> Future<V>.wait(wait: Wait = Wait.For(10, SECONDS)): V = when(wait) {
    is Wait.ForEver -> get()
    is Wait.For -> try {
        get(wait.time, wait.unit)
    } catch (e: TimeoutException) {
        error("Waited for $wait with no result")
    }
}

/**
 * [wait] with a recover option
 */
@Suppress("unused") // Public API
fun <V> Future<V>.waitOr(wait: Wait = Wait.For(10, SECONDS), alternative: (Throwable) -> V): V = try {
    wait(wait)
} catch (e: Throwable) {
    alternative(e)
}

/**
 * Describes waiting periods.
 */
sealed class Wait {

    /**
     * Mainly used for cases were we don't want to time out (i.e. debugging)
     */
    object ForEver: Wait() {
        override fun toString() = "forever"
    }

    /**
     * Default implementation to use for [Wait]
     */
    data class For(val time: Long, val unit: TimeUnit) : Wait() {
        override fun toString() = "$time ${unit.name.toLowerCase()}"
    }

}
