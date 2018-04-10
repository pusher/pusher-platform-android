package com.pusher.platform.test

import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.spek.api.dsl.TestBody
import org.jetbrains.spek.api.dsl.TestContainer
import org.jetbrains.spek.api.dsl.it

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
class SuspendedTestBody(private val timeout: Long, body: SuspendedTestBody.() -> Unit) : TestBody {

    private val conditions = mutableListOf<() -> SuspendTestResult>()
    private val start = System.currentTimeMillis()
    private val completeChannel = Channel<() -> Unit>()

    private fun duration() = System.currentTimeMillis() - start
    private fun isTimedOut() = duration() >= timeout
    private fun allConditionsPass() = conditions.all { it() === SuspendTestResult.Pass }
    private fun collateConditionMessages() =
        conditions.map { it() }
            .mapIndexed { i, result ->
                when (result) {
                    is SuspendTestResult.Pass -> "$i: Pass"
                    is SuspendTestResult.Failed -> "$i: Failed: \n${result.message}"
                }
            }
            .joinToString("\n")

    init {
        launch {
            while (!isTimedOut() && !allConditionsPass());
            done {
                when {
                    allConditionsPass() -> Unit
                    else -> error("Timed out before matching all conditions: \n${collateConditionMessages()}")
                }
            }
        }
        launch {
            body()
        }
    }

    fun until(condition: () -> SuspendTestResult) {
        conditions += condition
    }

    fun done(action: () -> Unit = {}) =
        completeChannel.offer(action)

    fun fail(cause: String) = done { error(cause) }

    fun attempt(action: () -> Unit) {
        try { action() }
        catch (e: Throwable) { done { throw e } }
    }

    suspend fun await() =
        completeChannel.receive().also {
            completeChannel.close()
        }

}

sealed class SuspendTestResult {
    object Pass : SuspendTestResult()
    data class Failed(val message: String) : SuspendTestResult()
}

/**
 * Entry point for [SuspendedTestBody]
 */
fun TestContainer.will(description: String, timeout: Long = 5000, body: SuspendedTestBody.() -> Unit) =
    it("will $description") {
        runBlocking { SuspendedTestBody(timeout, body).await() }()
    }
