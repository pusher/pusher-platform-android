package com.pusher.platform.network

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.pusher.util.Result
import com.pusher.util.asFailure
import com.pusher.util.asSuccess
import elements.Error
import elements.Errors
import elements.asSystemError
import java.io.Reader
import java.lang.reflect.Type

private val GSON = GsonBuilder()
    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
    .setLenient()
    .create()

inline fun <reified A> typeToken(): Type =
    object : TypeToken<A>() {}.type

internal inline fun <reified A> Reader.parseAs(type: Type = typeToken<A>()): Result<A, Error> =
    safeParse { GSON.fromJson<A>(this, type) }

internal inline fun <reified A> String.parseAs(): Result<A, Error> =
    safeParse { GSON.fromJson<A>(this, typeToken<A>()) }

internal fun <A> String.parseAs(type: Type): Result<A, Error> =
    safeParse { GSON.fromJson<A>(this, type) }

internal fun <A> String?.parseAs(type: Type, onMissing : () -> Error): Result<A, Error> = when(this) {
    null -> onMissing().asFailure()
    else -> parseAs<A>(type).flatRecover { error ->
        Errors.other("Could not parse body: $this", error.asSystemError()).asFailure()
    }
}

internal inline fun <reified A> String?.parseAs(noinline onMissing : () -> Error): Result<A, Error> =
    parseAs(typeToken<A>(), onMissing)

internal inline fun <reified A> JsonElement.parseAs(): Result<A, Error> = safeParse {
    GSON.fromJson<A>(this, typeToken<A>())
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
