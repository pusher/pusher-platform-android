package com.pusher.platform.test

import com.pusher.platform.SubscriptionListeners

/**
 * Counts interactions.
 */
private data class Counter(
    var open: Int = 0,
    var events: Int = 0,
    var end: Int = 0,
    var error: Int = 0,
    var retrying: Int = 0,
    var subscribe: Int = 0
)

/**
 * Used to verify if a given [Counter] meets expectations and to describe an error when it doesn't.
 */
private class InteractionExpectation(val expected: Counter) {

    fun verify(actual: Counter): Boolean =
        expected == actual

    private fun Int.describe() = when (this) {
        0 -> "never called"
        1 -> "called once"
        else -> "called $this times"
    }

    private fun describe(name: String, expected: Int, actual: Int) = when {
        expected != actual -> "\tExpected '$name' to be ${expected.describe()} but was ${actual.describe()}.\n"
        else -> ""
    }

    fun describe(message: String, actual: Counter): String = "Failed ($message): \n" +
        describe("onOpen", expected.open, actual.open) +
        describe("onEvents", expected.events, actual.events) +
        describe("onEnd", expected.end, actual.end) +
        describe("onError", expected.error, actual.error) +
        describe("onRetrying", expected.retrying, actual.retrying) +
        describe("onSubscribe", expected.subscribe, actual.subscribe)

}

/**
 * Extension on [SuspendedTestBody] that will observe until the provided expectations are met or it times out.
 * We can provide an optional time out in milliseconds which is 100 by default.
 */
fun SuspendedTestBody.observeUntil(
    open: Int = 0,
    events: Int = 0,
    end: Int = 0,
    error: Int = 0,
    retrying: Int = 0,
    subscribe: Int = 0 ,
    timeout: Int = 100
): SubscriptionListeners {

    val expected = Counter(open, events, end, error, retrying, subscribe)
    val actual = Counter()
    val expectation = InteractionExpectation(expected)

    timeout(timeout) {
        if (!expectation.verify(actual)) {
            error(expectation.describe("timeout", actual))
        }
    }

    fun increment(message: String, action: (Counter) -> Unit) {
        println(message)
        action(actual)
        if (expectation.verify(actual)) done()
    }

    return SubscriptionListeners(
        onOpen = { increment("onOpen($it)") { it.open++ } },
        onEnd = { increment("onEnd($it)") { it.end++ } },
        onEvent = { increment("onEvent($it)") { it.events++ } },
        onError = { increment("onError($it)") { it.error++ } },
        onRetrying = { increment("onRetrying()") { it.retrying++ } },
        onSubscribe = { increment("onSubscribe()") { it.subscribe++ } }
    )

}
