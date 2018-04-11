package com.pusher.platform

import java.util.concurrent.BlockingQueue
import java.util.concurrent.SynchronousQueue
import kotlin.reflect.KProperty

/**
 * Delegate for a property that will be set later, using the delegated property will block current
 * thread until the value is set.
 */
class FutureValue<A> {
    private val queue: BlockingQueue<A> = SynchronousQueue<A>(true)
    operator fun getValue(thisRef: Nothing?, property: KProperty<*>): A = queue.take()
    operator fun setValue(thisRef: Nothing?, property: KProperty<*>, value: A) = queue.put(value)
}
