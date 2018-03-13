package com.pusher.platform.network

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import java.io.Reader

private val GSON = GsonBuilder()
    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
    .create()

internal inline fun <reified A> Reader.parseAs() =
    GSON.fromJson(this, A::class.java)

internal inline fun <reified A> String.parseAs() =
    GSON.fromJson(this, A::class.java)

internal inline fun <reified A> JsonElement.parseAs() =
    GSON.fromJson(this, A::class.java)

internal inline fun <reified A> Reader?.parseOr(f: () -> A ) =
    this?.parseAs() ?: f()

internal inline fun <reified A> JsonElement?.parseOr(f: () -> A ) =
    this?.parseAs() ?: f()