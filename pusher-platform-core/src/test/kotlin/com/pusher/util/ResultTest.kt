package com.pusher.util

import com.google.common.truth.Truth.assertThat
import com.pusher.platform.network.Futures
import com.pusher.platform.network.toFuture
import com.pusher.platform.network.wait
import elements.Error
import elements.Errors
import org.junit.jupiter.api.Test

private const val SUCCESS_VALUE = "value"
private const val ALTERNATIVE_VALUE = 2.0f
private val FAILURE_VALUE = Errors.other("an error")
private val SUCCESS_RESULT = Result.success<String, Error>(SUCCESS_VALUE)
private val FAILURE_RESULT = Result.failure<String, Error>(FAILURE_VALUE)
private val ALTERNATIVE_RESULT = Result.success<Float, Error>(ALTERNATIVE_VALUE)

class ResultTest {

    @Test
    fun `success instantiation`() {
        val result = SUCCESS_VALUE.asSuccess<String, Error>()

        assertThat(result).isEqualTo(SUCCESS_RESULT)
    }

    @Test
    fun `failure instantiation`() {
        val result = FAILURE_VALUE.asFailure<String, Error>()

        assertThat(result).isEqualTo(FAILURE_RESULT)
    }

    @Test
    fun `success from nullable`() {
        val result = SUCCESS_VALUE.orElse { FAILURE_VALUE }

        assertThat(result).isEqualTo(SUCCESS_RESULT)
    }

    @Test
    fun `failure from nullable`() {
        val result = null.orElse { FAILURE_VALUE }

        assertThat(result).isEqualTo(FAILURE_RESULT)
    }

    @Test
    fun `maps to another success result`() {
        val result = SUCCESS_RESULT.map { ALTERNATIVE_VALUE }

        assertThat(result).isEqualTo(ALTERNATIVE_RESULT)
    }

    @Test
    fun `map keeps failure result`() {
        val result = FAILURE_RESULT.map { ALTERNATIVE_VALUE }

        assertThat(result).isEqualTo(FAILURE_RESULT)
    }

    @Test
    fun `flatMaps to success result`() {
        val result = SUCCESS_RESULT.flatMap { ALTERNATIVE_RESULT }

        assertThat(result).isEqualTo(ALTERNATIVE_RESULT)
    }

    @Test
    fun `flatMap keeps failure result`() {
        val result = FAILURE_RESULT.flatMap { ALTERNATIVE_RESULT }

        assertThat(result).isEqualTo(FAILURE_RESULT)
    }

    @Test
    fun `recover should not change success result`() {
        val result = SUCCESS_RESULT.recover { "invalid" }

        assertThat(result).isEqualTo(SUCCESS_VALUE)
    }

    @Test
    fun `recover should provide alternative on failure`() {
        val result = FAILURE_RESULT.recover { "other success" }

        assertThat(result).isEqualTo("other success")
    }

    @Test
    fun `recover should provide result alternative on failure`() {
        val result = FAILURE_RESULT.flatRecover { SUCCESS_RESULT }

        assertThat(result).isEqualTo(SUCCESS_RESULT)
    }

    @Test
    fun `flatten successful success`() {
        val result = SUCCESS_RESULT.asSuccess<Result<String, Error>, Error>()

        val flatten = result.flatten()
        assertThat(flatten).isEqualTo(SUCCESS_RESULT)
    }

    @Test
    fun `flatten failure success`() {
        val result = FAILURE_RESULT.asSuccess<Result<String, Error>, Error>()

        val flatten = result.flatten()
        assertThat(flatten).isEqualTo(FAILURE_RESULT)
    }

    @Test
    fun `flatten failure`() {
        val result = FAILURE_VALUE.asFailure<Result<String, Error>, Error>()

        val flatten = result.flatten()
        assertThat(flatten).isEqualTo(FAILURE_RESULT)
    }

    @Test
    fun `collect successes`() {
        val results = Result.successesOf(
            "a".asSuccess<String, Error>(),
            "b".asSuccess<String, Error>(),
            Errors.other("b").asFailure())

        assertThat(results).containsExactly("a", "b")
    }

    @Test
    fun `collect failures`() {
        val results = Result.failuresOf(
            "a".asSuccess(),
            "b".asSuccess(),
            Errors.other("b").asFailure<String, Error>())

        assertThat(results).containsExactly(Errors.other("b"))
    }

    @Test
    fun `future maps success result`() {
        val future = Futures.now(Result.success<String, Error>("a"))

        val mapped = future.mapResult { it + "b" }.wait()

        assertThat(mapped).isEqualTo("ab".asSuccess<String, Error>())
    }

    @Test
    fun `future maps failure result`() {
        val future = Futures.now(FAILURE_RESULT)

        val mapped = future.mapResult { it + "b" }.wait()

        assertThat(mapped).isEqualTo(FAILURE_RESULT)
    }

    @Test
    fun `future recovers from failure result`() {
        val future = Futures.now(FAILURE_RESULT)

        val recovered = future.recoverResult { SUCCESS_RESULT }.wait()

        assertThat(recovered).isEqualTo(SUCCESS_RESULT)
    }

    @Test
    fun `future recovers from success result`() {
        val future = Futures.now(SUCCESS_RESULT)

        val recovered = future.recoverResult { error("no recovery needed") }.wait()

        assertThat(recovered).isEqualTo(SUCCESS_RESULT)
    }

    @Test
    fun `recover future failure`() {
        val future = Futures.now(FAILURE_RESULT)

        val recovered = future.recoverFutureResult { SUCCESS_RESULT.toFuture() }.wait()

        assertThat(recovered).isEqualTo(SUCCESS_RESULT)
    }

    @Test
    fun `recover future success`() {
        val future = Futures.now(SUCCESS_RESULT)

        val recovered = future.recoverFutureResult { error("no recovery needed") }.wait()

        assertThat(recovered).isEqualTo(SUCCESS_RESULT)
    }

    @Test
    fun `flatMap future success to result`() {
        val future = Futures.now(SUCCESS_RESULT)

        val flatMapped = future.flatMapResult { "b".asSuccess<String, Error>() }.wait()

        assertThat(flatMapped).isEqualTo("b".asSuccess<String, Error>())
    }

    @Test
    fun `flatMap future failure to result`() {
        val future = Futures.now(FAILURE_RESULT)

        val flatMapped = future.flatMapResult<String, Error, String> { error("no flatMap expected") }.wait()

        assertThat(flatMapped).isEqualTo(FAILURE_RESULT)
    }

    @Test
    fun `flatMap future success to future result`() {
        val future = Futures.now(SUCCESS_RESULT)

        val flatMapped = future.flatMapFutureResult { "b".asSuccess<String, Error>().toFuture() }.wait()

        assertThat(flatMapped).isEqualTo("b".asSuccess<String, Error>())
    }

    @Test
    fun `flatMap future failure to future result`() {
        val future = Futures.now(FAILURE_RESULT)

        val flatMapped = future.flatMapFutureResult<String, Error, String> { error("no flatMap expected") }.wait()

        assertThat(flatMapped).isEqualTo(FAILURE_RESULT)
    }

}
