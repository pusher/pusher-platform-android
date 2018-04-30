package com.pusher.platform.network

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.pusher.util.Result
import com.pusher.util.asFailure
import com.pusher.util.asSuccess
import elements.Error
import elements.Errors
import elements.asSystemError
import java.io.Reader

private val GSON = GsonBuilder()
    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
    .setLenient()
    .create()

internal inline fun <reified A> Reader.parseAs(): Result<A, Error> = safeParse {
    GSON.fromJson(this, A::class.java)
}

internal inline fun <reified A> String.parseAs(): Result<A, Error> =
    parseAs(A::class.java)

internal fun <A> String.parseAs(type: Class<A>): Result<A, Error> = safeParse {
    GSON.fromJson(this, type)
}

internal inline fun <reified A> String?.parseAs(noinline onMissing : () -> Error): Result<A, Error> =
    parseAs(A::class.java, onMissing)

internal fun <A> String?.parseAs(type: Class<A>, onMissing : () -> Error): Result<A, Error> = when(this) {
    null -> onMissing().asFailure()
    else -> safeParse { GSON.fromJson(this, type) }
        .flatRecover { error ->
            Errors.other("Could not parse body: $this", error.asSystemError()).asFailure<A, Error>()
        }
}

internal inline fun <reified A> JsonElement.parseAs(): Result<A, Error> = safeParse {
    GSON.fromJson(this, A::class.java)
}

internal inline fun <reified A> Reader?.parseOr(f: () -> A): Result<A, Error> =
    this?.parseAs() ?: f().asSuccess()

internal inline fun <reified A> JsonElement?.parseOr(f: () -> A): Result<A, Error> =
    this?.parseAs() ?: f().asSuccess()

private fun <A> safeParse(block: () -> A): Result<A, Error> = try {
    block().asSuccess()
} catch (e: Throwable) {
    Errors.other(e).asFailure()
}
