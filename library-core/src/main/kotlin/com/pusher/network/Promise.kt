package com.pusher.network

import com.pusher.annotations.UsesCoroutines
import kotlinx.coroutines.experimental.channels.Channel
import java.util.concurrent.ConcurrentLinkedQueue

typealias FutureResultListener<A> = (A) -> Unit

fun <A> A.asPromise(): Promise<A> = Promise.Now(this)

/**
 * Inspired by [java.util.concurrent.Future] but with better support for older versions of Android.
 */
sealed class Promise<out A> {

    companion object {
        fun <A> promise(block: PromiseContext<A>.() -> Unit): Promise<A> = Pending(block)
        fun <A> now(value: A): Promise<A> = Now(value)
    }

    class PromiseContext<A> {

        private var result: State<A> = State.NotReady()
        private var cancelled = false

        private val cancelListeners = ConcurrentLinkedQueue<() -> Unit>()
        private val listeners = ConcurrentLinkedQueue<FutureResultListener<A>>()

        /**
         * Used to report that a value is available
         */
        fun report(value: A) {
            result = State.Ready(value)
            listeners.forEach { it(value) }
        }

        /**
         * Attaches a listener for when [cancel] is called
         */
        fun onCancel(listener: () -> Unit) {
            cancelListeners += listener
            if (cancelled) {
                listener()
            }
        }

        internal fun onReady(listener: (A) -> Unit) {
            listeners += listener
            (result as? State.Ready<A>)?.run { listener(value) }
        }

        internal fun cancel() {
            cancelled = true
            cancelListeners.forEach { it() }
        }

        fun isReady(): Boolean =
            result is State.Ready<A>

        private sealed class State<out A> {
            class NotReady<out A>: State<A>()
            data class Ready<out A>(val value: A) : State<A>()
        }

    }

    /**
     * Use this to register a listener that will recieve an update when the value is available,
     * which could be at the time of invocation.
     */
    abstract fun onReady(listener: FutureResultListener<A>)

    /**
     * @return true if the promise has a value available
     */
    abstract fun isReady(): Boolean

    /**
     * This will signal the original creator of the promise that it needs to cancel. This has no
     * effect on delivery as if the promise is ready it will still provide results when observed.
     */
    abstract fun cancel()

    fun <B> map(block: (A) -> B): Promise<B> =
        Mapped(this, block)

    fun <B> flatMap(block: (A) -> Promise<B>): Promise<B> =
        FlatMapped(this, block)

    class Pending<out A>(block: PromiseContext<A>.() -> Unit) : Promise<A>() {

        private val context = PromiseContext<A>()

        init {
            context.block()
        }

        override fun onReady(listener: FutureResultListener<A>) = context.onReady(listener)
        override fun cancel() = context.cancel()
        override fun isReady(): Boolean = context.isReady()
    }

    data class Now<out A>(private val result: A) : Promise<A>() {
        override fun onReady(listener: FutureResultListener<A>) = listener(result)
        override fun cancel() = Unit
        override fun isReady(): Boolean = true
    }

    private data class Mapped<A, out B>(
        val original: Promise<A>,
        val block: (A) -> B
    ) : Promise<B>() {
        override fun onReady(listener: FutureResultListener<B>) =
            original.onReady { listener(block(it)) }
        override fun isReady(): Boolean = original.isReady()
        override fun cancel() = original.cancel()
    }

    private data class FlatMapped<A, out B>(
        val original: Promise<A>,
        val block: (A) -> Promise<B>
    ) : Promise<B>() {
        override fun onReady(listener: FutureResultListener<B>) =
            original.onReady { block(it).onReady(listener) }
        override fun cancel() = original.cancel()
        override fun isReady(): Boolean = original.isReady()
    }

}

@UsesCoroutines
suspend fun <A> Promise<A>.await(): A = with(Channel<A>()) {
    onReady { offer(it) }
    receive()
}
