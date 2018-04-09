package com.pusher.platform.test

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.jetbrains.spek.api.dsl.TestBody
import org.jetbrains.spek.api.dsl.TestContainer
import org.jetbrains.spek.api.dsl.it
import java.util.concurrent.LinkedBlockingQueue

/**
 * Spek extension to allow a test to block for threaded completion. It can be used inside a
 * [TestContainer] by using the function `will("do something") {}`.
 *
 * To suspend the test we need to call `await()`.
 *
 * To finish the suspension we can either call `done()` or `done {}`  with an action (i.e. fail test)
 * as well as (and recommended to avoid endless tests) with the function `timeout {}` to provide
 * with an action if the test takes too long (default is 100ms).
 */
class SuspendedTestBody : TestBody {

    private var isDone = false
    private val queue = LinkedBlockingQueue<() -> Unit>()

    /**
     *
     */
    fun await() = queue.poll().invoke()

    fun done(action: () -> Unit = {}) {
        if (!isDone) queue.add(action)
        isDone = true
    }

    /**
     * Will trigger the provided action after [timeout] is the test is still running.
     */
    fun timeout(timeout: Int = 100, action: () -> Unit) = launch {
        delay(timeout)
        done(action)
    }

}

/**
 * Entry point for [SuspendedTestBody]
 */
fun TestContainer.will(description: String, timeout: Int = 100, body: SuspendedTestBody.() -> Unit) =
    it("will $description") {
        val testBody = SuspendedTestBody()
        testBody.body()
        testBody.await()
        launch {
            delay(timeout)
            testBody.done { error("Timed out after: $timeout millis") }
        }
    }
