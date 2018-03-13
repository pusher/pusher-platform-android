package com.pusher.network

import com.pusher.annotations.UsesCoroutines
import kotlinx.coroutines.experimental.channels.Channel
import java.util.concurrent.ConcurrentLinkedQueue

typealias FutureResultListener<A> = (A) -> Unit

fun <A> A.asPromise(): Promise<A> = Promise.Now(this)

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

        fun report(value: A) {
            result = State.Ready(value)
            listeners.forEach { it(value) }
        }

        fun onCancel(listener: () -> Unit) {
            cancelListeners += listener
            if (cancelled) {
                listener()
            }
        }

        fun onReady(listener: (A) -> Unit) {
            listeners += listener
            (result as? State.Ready<A>)?.run { listener(value) }
        }

        fun cancel() {
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

    abstract fun onReady(listener: FutureResultListener<A>)
    abstract fun isReady(): Boolean
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
