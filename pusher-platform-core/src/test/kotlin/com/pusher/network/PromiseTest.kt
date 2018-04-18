package com.pusher.network

import com.google.common.truth.Truth.assertThat
import com.pusher.platform.FutureValue
import com.pusher.platform.network.Promise
import com.pusher.platform.network.asPromise
import kotlinx.coroutines.experimental.launch
import org.junit.jupiter.api.Test

private const val EXPECTED_RESULT = "expected result"

class PromiseTest {

    @Test
    fun `should call onReady when value reported`() {
        var result: String? = null
        val promise = Promise.promise<String> { report(EXPECTED_RESULT) }

        promise.onReady { result = it }

        assertThat(result).isEqualTo(EXPECTED_RESULT)
    }

    @Test
    fun `should report as ready with delayed execution`() {
        var collector by FutureValue<String>()
        var reporter by FutureValue<String>()

        val promise = Promise.promise<String> {
            launch {
                report(reporter)
            }
        }

        promise.onReady { collector = it }

        reporter = EXPECTED_RESULT

        assertThat(collector).isEqualTo(EXPECTED_RESULT)
    }

    @Test
    fun `should call on ready when register with initial value`() {
        var result: String? = null
        val promise = EXPECTED_RESULT.asPromise()

        promise.onReady { result = it }

        assertThat(result).isEqualTo(EXPECTED_RESULT)
    }

    @Test
    fun `should not be ready if not reported`() {
        val promise = Promise.promise<String> {}

        assertThat(promise.isReady()).isFalse()
    }

    @Test
    fun `should be ready after reporting`() {
        val promise = Promise.promise<String> { report(EXPECTED_RESULT) }

        assertThat(promise.isReady()).isTrue()
    }

    @Test
    fun `should call cancellation listener when cancelled`() {
        var cancelled = false
        val promise = Promise.promise<String> { onCancel { cancelled = true } }

        promise.cancel()

        assertThat(cancelled).isTrue()
    }

    @Test
    fun `should call cancellation listener if cancelled before`() {
        var cancelled = false

        Promise.promise<String> {
            cancel()
            onCancel { cancelled = true }
        }

        assertThat(cancelled).isTrue()
    }

    @Test
    fun `should map promise`() {
        var result: Int = -1
        val promise = Promise.now(EXPECTED_RESULT).map { 123 }

        promise.onReady { result = it }

        assertThat(result).isEqualTo(123)
    }

    @Test
    fun `should map pending promise`() {
        var result: Int = -1
        val promise = Promise.promise<String> { report(EXPECTED_RESULT) }.map { 123 }

        promise.onReady { result = it }

        assertThat(result).isEqualTo(123)
    }

    @Test
    fun `should flatMap promise`() {
        var result: Int = -1
        val promise = Promise.now(EXPECTED_RESULT).flatMap { Promise.now(123) }

        promise.onReady { result = it }

        assertThat(result).isEqualTo(123)
    }

    @Test
    fun `should flatMap pending promise`() {
        var result: Int = -1
        val promise = Promise.promise<String> { report(EXPECTED_RESULT) }.flatMap { Promise.now(123) }

        promise.onReady { result = it }

        assertThat(result).isEqualTo(123)
    }

}
