package com.pusher.util

import com.pusher.platform.network.flatMap
import com.pusher.platform.network.map
import com.pusher.platform.network.toFuture
import com.pusher.util.Result.Companion.failure
import com.pusher.util.Result.Companion.success
import elements.Errors
import elements.Error
import java.util.concurrent.Future

fun <A, B: Error> A.asSuccess(): Result<A, B> = success(this)
fun <A, B: Error> B.asFailure(): Result<A, B> = failure(this)
fun <A, B: Error> A?.orElse(block: () -> B): Result<A, B> = when (this) {
    null -> failure(block())
    else -> success(this)
}

/**
 * Slight modification of the Either monadic pattern with semantics of failure and success for
 * left and right respectively.
 *
 * A [Result] instance can be instantiated:
 *  - Using one of the factory methods: `Result.success(value)` or `Result.failure(error)`
 *  - Extension functions: `value.asSuccess()` or `error.asFailure()`
 *  - From a nullable: `candidate.orElse { error }`
 */
sealed class Result<A, B: Error> {

    @Suppress("MemberVisibilityCanBePrivate") // public APIs
    companion object {
        @JvmStatic
        fun <A, B: Error> success(value: A): Result<A, B> = Success(value)
        @JvmStatic
        fun <A, B: Error> failure(error: B): Result<A, B> = Failure(error)
        @JvmStatic
        fun <B: Error> failuresOf(vararg results: Result<*, B>): List<B> =
            failuresOf(results.asList())
        @JvmStatic
        fun <B: Error> failuresOf(results: Iterable<Result<*, B>>): List<B> =
            results.mapNotNull { it as? Result.Failure }.map { it.error }
        @JvmStatic
        fun <A> successesOf(vararg results: Result<A, *>): List<A> =
            successesOf(results.asList())
        @JvmStatic
        fun <A> successesOf(results: Iterable<Result<A, *>>): List<A> =
            results.mapNotNull { it as? Result.Success }.map { it.value }
    }

    data class Success<A, B: Error> internal constructor(val value: A) : Result<A, B>()
    data class Failure<A, B: Error> internal constructor(val error: B) : Result<A, B>()

    inline fun <C> fold(onFailure: (B) -> C, onSuccess: (A) -> C): C = when (this) {
        is Failure -> onFailure(error)
        is Success -> onSuccess(value)
    }

    /**
     * Creates a new result using [block] to convert when it is success.
     */
    inline fun <C> map(block: (A) -> C): Result<C, B> = fold(
        onFailure = { failure(it) },
        onSuccess = { success(block(it)) }
    )

    /**
     * Creates a new result using [block] to convert when it is success.
     */
    inline fun <C: Error> mapFailure(block: (B) -> C): Result<A, C> = fold(
            onFailure = { failure(block(it)) },
            onSuccess = { it.asSuccess() }
    )

    /**
     * Creates a new result when it is a failure or uses [block] to create a new result.
     */
    inline fun <C> flatMap(block: (A) -> Result<C, B>): Result<C, B> = fold(
        onFailure = { failure(it) },
        onSuccess = { block(it) }
    )

    /**
     * If the result is a failure it will create a new success result using the provided [block].
     */
    fun recover(block: (B) -> A): A = fold(
        onFailure = block,
        onSuccess = { it }
    )

    /**
     * Similar to [recover] but allows to create a new result instance with [block] instead of
     * always recovering with a success.
     */
    fun flatRecover(block: (B) -> Result<A, B>): Result<A, B> = fold(
        onFailure = block,
        onSuccess = { it.asSuccess() }
    )

}

/**
 * Collapses a nested result into a simple one
 */
fun <A, B: Error> Result<Result<A, B>, B>.flatten(): Result<A, B> = fold(
    onFailure = { it.asFailure() },
    onSuccess = { it }
)

/**
 * Collapses a nested result into a simple one
 */
fun <B: Error> Result<B, B>.flatten(): B = fold(
    onFailure = { it },
    onSuccess = { it }
)

/**
 * [Result.map] when result is inside a [Future].
 */
fun <A, B: Error, C> Future<Result<A, B>>.mapResult(block: (A) -> C): Future<Result<C, B>> =
    map { it.map(block) }

/**
 * [Result.recover] when result is inside a [Future].
 */
fun <A, B: Error> Future<Result<A, B>>.recoverResult(block: (B) -> Result<A, B>): Future<Result<A, B>> =
    map {
        it.fold(
            onFailure = { block(it) },
            onSuccess = { it.asSuccess() }
        )
    }

/**
 * Same as [recoverResult] but recovering with a future.
 */
fun <A, B: Error> Future<Result<A, B>>.recoverFutureResult(block: (B) -> Future<Result<A, B>>): Future<Result<A, B>> =
    flatMap {
        it.fold(
            onFailure = { block(it) },
            onSuccess = { it.asSuccess<A, B>().toFuture() }
        )
    }

/**
 * [Result.flatMap] when result is inside a [Future].
 */
fun <A, B: Error, C> Future<Result<A, B>>.flatMapResult(block: (A) -> Result<C, B>): Future<Result<C, B>> =
    map { it.flatMap(block) }

/**
 * Same as [flatMapResult] but returning a future when mapping.
 */
fun <A, B: Error, C> Future<Result<A, B>>.flatMapFutureResult(block: (A) -> Future<Result<C, B>>) : Future<Result<C, B>> =
    flatMap {
        it.fold(
            onFailure = { it.asFailure<C, B>().toFuture() },
            onSuccess = { block(it) }
        )
    }

/**
 * Transforms a list of results in to a result of a List.
 * Errors are composed using elements.Errors.compose()
 */
fun <A, B: Error> Iterable<Result<A, B>>.collect(): Result<List<A>, Error> =
    if (Result.failuresOf(this).isEmpty()) {
        Result.successesOf(this).asSuccess()
    } else {
        Errors.compose(Result.failuresOf(this)).asFailure()
    }
