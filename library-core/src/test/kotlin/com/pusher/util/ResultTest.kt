package com.pusher.util

import com.google.common.truth.Truth.assertThat
import com.pusher.platform.network.asPromise
import com.pusher.platform.FutureValue
import org.junit.Test

private const val SUCCESS_VALUE = "value"
private const val FAILURE_VALUE = 123
private const val ALTERNATIVE_VALUE = 2.0f
private val SUCCESS_RESULT = Result.Success<String, Int>(SUCCESS_VALUE)
private val FAILURE_RESULT = Result.Failure<String, Int>(FAILURE_VALUE)
private val ALTERNATIVE_RESULT = Result.Success<Float, Int>(ALTERNATIVE_VALUE)

class ResultTest {

    @Test
    fun `success instantiation`() {
        val result= SUCCESS_VALUE.asSuccess<String, Int>()

        assertThat(result).isEqualTo(SUCCESS_RESULT)
    }

    @Test
    fun `failure instantiation`() {
        val result = FAILURE_VALUE.asFailure<String, Int>()

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
    fun `map promise with result`() {
        var result by FutureValue<Any>()
        val promise = SUCCESS_RESULT.asPromise().mapResult { "promised" }

        promise.onReady { result = it }

        assertThat(result).isEqualTo("promised".asSuccess<String, Int>())
    }

}
