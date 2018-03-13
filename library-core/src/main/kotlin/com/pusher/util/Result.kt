package com.pusher.util

import com.pusher.annotations.UsesCoroutines
import com.pusher.network.Promise
import com.pusher.network.asPromise
import com.pusher.util.Result.Companion.failure
import com.pusher.util.Result.Companion.success

fun <A, B> A.asSuccess(): Result<A, B> = success(this)
fun <A, B> B.asFailure(): Result<A, B> = failure(this)
fun <A, B> A?.orElse(block: () -> B): Result<A, B> = when (this) {
    null -> failure(block())
    else -> success(this)
}

/**
 * Slight modification of the Either monadic pattern with semantics of failure and success for
 * left and right respectively.
 */
sealed class Result<A, B> {

    companion object {
        fun <A, B> success(value: A) = Success<A, B>(value)
        fun <A, B> failure(error: B) = Failure<A, B>(error)
    }

    data class Success<A, B>(val value: A) : Result<A, B>()
    data class Failure<A, B>(val error: B) : Result<A, B>()

    inline fun <C> fold(onFailure: (B) -> C, onSuccess: (A) -> C): C = when (this) {
        is Failure -> onFailure(error)
        is Success -> onSuccess(value)
    }

    inline fun <C> map(block: (A) -> C): Result<C, B> = fold(
        onFailure = { failure(it) },
        onSuccess = { success(block(it)) }
    )

    inline fun <C> flatMap(block: (A) -> Result<C, B>): Result<C, B> = fold(
        onFailure = { failure(it) },
        onSuccess = { block(it) }
    )

    fun swap() : Result<B, A> = fold(
        onFailure = { it.asSuccess() },
        onSuccess = { it.asFailure() }
    )

    fun recover(block: (B) -> A) : A = fold(
        onFailure = block,
        onSuccess = { it }
    )

    fun flatRecover(block: (B) -> Result<A, B>) : Result<A, B> = fold(
        onFailure = block,
        onSuccess = { it.asSuccess() }
    )

}

/**
 * Short for `map { it.map(block) }`
 */
fun <A, B, C> Promise<Result<A, B>>.mapResult(block: (A) -> C) : Promise<Result<C, B>> =
    map { it.map(block) }

/**
 * Short for `flatMap { it.map(block).recover { it.asFailure<C, B>().asPromise() } }`
 */
fun <A, B, C> Promise<Result<A, B>>.flatMapResult(block: (A) -> Promise<Result<C, B>>) : Promise<Result<C, B>> =
    flatMap { it.map(block).recover { it.asFailure<C, B>().asPromise() } }

/**
 * Short for `map { it.fold(onFailure, onSuccess) }`
 */
fun <A, B, C> Promise<Result<A, B>>.fold(onFailure: (B) -> C, onSuccess: (A) -> C) : Promise<C> =
    map { it.fold(onFailure, onSuccess) }

/**
 * Short for `map { it.swap() }`
 */
fun <A, B> Promise<Result<A, B>>.swap() : Promise<Result<B, A>> =
    map { it.swap() }

/**
 * Short for `map { it.recover(block) }`
 */
fun <A, B> Promise<Result<A, B>>.recover(block: (B) -> A) : Promise<A> =
    map { it.recover(block) }

/**
 * Short for `map { it.recover(block) }`
 */
fun <A, B> Promise<Result<A, B>>.flatRecover(block: (B) -> Result<A, B>) : Promise<Result<A, B>> =
    map { it.flatRecover(block) }


@UsesCoroutines
fun <A, B> Result<A, B>.async(): SuspendedResult<A, B> =
    SuspendedResult(this)

@UsesCoroutines
data class SuspendedResult<A, B> internal constructor(private val result: Result<A, B>) {

    suspend fun <C> fold(onFailure: suspend (B) -> C, onSuccess: suspend (A) -> C): C =
        result.fold(onFailure.desuspend(), onSuccess.desuspend()).invoke()

    suspend fun <C> map(block: suspend (A) -> C): SuspendedResult<C, B> =
        result.map { block(it) }.async()

    suspend fun <C> flatMap(block: suspend (A) -> Result<C, B>): SuspendedResult<C, B> =
        result.flatMap { block(it) }.async()

    private fun <A, B> (suspend (A) -> B).desuspend(): (A) -> suspend () -> B = { suspend { this(it) } }

}
