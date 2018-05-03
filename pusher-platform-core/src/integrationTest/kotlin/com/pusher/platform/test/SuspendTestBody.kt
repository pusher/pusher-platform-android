package com.pusher.platform.test

import com.pusher.platform.network.Futures
import com.pusher.platform.network.Wait
import com.pusher.platform.network.wait
import org.jetbrains.spek.api.dsl.TestBody
import org.jetbrains.spek.api.dsl.TestContainer
import org.jetbrains.spek.api.dsl.it
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.TimeUnit.SECONDS


/**
 * Spek extension to allow a test to block for threaded completion. It can be used inside a
 * [TestContainer] by using the function `will("do something") {}`.
 *
 * To suspend the test we need to call `await()`.
 *
 * To finish the suspension we can either call `done()`, `done {}` with an action (i.e. fail test)
 * or `fail(cause)`.
 *
 */
class SuspendedTestBody(body: SuspendedTestBody.() -> Unit) : TestBody {

    private val pendingAction = SynchronousQueue<() -> Unit>()

    init {
        Futures.schedule { body() }
    }

    /**
     * signals the test to stop with an action that could fail. This will run on the original thread.
     */
    fun done(action: () -> Unit = {}) =
        pendingAction.offer(action)

    /**
     * Signals the test to fail with the provided [cause]
     */
    fun fail(cause: String) = done { throw Error(cause) }

    /**
     * Tries to run the provided [action] failing the test if it throws an exception.
     */
    fun attempt(action: () -> Unit) {
        try {
            action()
        } catch (e: Throwable) {
            done { throw e }
        }
    }

    /**
     * Waits for [done], [fail] or [attempt] with failure are called.
     */
    fun await(wait: Wait = Wait.For(5, SECONDS)): () -> Unit = Futures.schedule {
        pendingAction.take()
    }.wait(wait)

}

/**
 * Main entry point for [SuspendedTestBody], use this to create a test that will wait for async termination or [wait]
 */
fun TestContainer.will(
    description: String,
    wait: Wait = Wait.For(5, SECONDS),
    body: SuspendedTestBody.() -> Unit
): Unit = it("will $description") {
     SuspendedTestBody(body).await(wait).let { complete ->
         complete()
     }
}
