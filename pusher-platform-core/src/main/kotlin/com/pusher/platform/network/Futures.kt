package com.pusher.platform.network

import java.util.concurrent.*
import java.util.concurrent.TimeUnit.*

private val futuresExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

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

fun <A> A.toFuture(): Future<A> =
    Futures.now(this)

fun <V, R> Future<V>.map(block: (V) -> R) =
    Futures.map(this, block)

fun <V, R> Future<V>.flatMap(block: (V) -> Future<R>) =
    Futures.flatMap(this, block)

fun <V> Future<V>.cancel() = cancel(true)

private class MapFuture<V, R>(val future: Future<V>, val block: (V) -> R) : Future<R> {
    override fun isDone() = future.isDone
    override fun get(): R = future.get().let(block)
    override fun get(time: Long, unit: TimeUnit?): R = future.get(time, unit).let(block)
    override fun cancel(mayInterruptIfRunning: Boolean) = future.cancel(mayInterruptIfRunning)
    override fun isCancelled() = future.isCancelled
}

private class FlatMapFuture<V, R>(val future: Future<V>, val block: (V) -> Future<R>) : Future<R> {
    override fun isDone() = future.isDone
    override fun get(): R = future.get().let(block).get()
    override fun get(time: Long, unit: TimeUnit?): R = future.get(time, unit).let(block).get(time, unit)
    override fun cancel(mayInterruptIfRunning: Boolean) = future.cancel(mayInterruptIfRunning)
    override fun isCancelled() = future.isCancelled
}


/**
 * Adds the option to use a [Future] as a blocking delegated property.
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
fun <V> Future<V>.waitOr(wait: Wait = Wait.For(10, SECONDS), alternative: (Throwable) -> V): V = try {
    wait(wait)
} catch (e: Throwable) {
    alternative(e)
}

/**
 * Describes waiting periods.
 */
sealed class Wait {

    object ForEver: Wait() {
        override fun toString() = "forever"
    }

    data class For(val time: Long, val unit: TimeUnit) : Wait() {
        override fun toString() = "$time ${unit.name.toLowerCase()}"
    }

}
