package elements

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.pusher.platform.network.parseAs
import com.pusher.platform.subscription.SubscriptionTypeResolver
import com.pusher.util.Result
import com.pusher.util.Result.Companion.failuresOf
import com.pusher.util.asFailure
import com.pusher.util.asSuccess
import com.pusher.util.orElse
import java.lang.reflect.Type

internal fun <A> String.toSubscriptionMessage(typeResolver: SubscriptionTypeResolver): Result<SubscriptionMessage<A>, Error> =
    parseAs<Array<JsonElement>>().flatMap {
        when (it.messageType) {
            0 -> it.createControlEvent()
            1 -> it.createSubscriptionEvent<A>(typeResolver)
            255 -> it.createEosEvent()
            else -> Errors.other("Unknown message type: $this").asFailure()
        }
    }

@Suppress("unused")
sealed class SubscriptionMessage<out A>

data class SubscriptionEvent<out A>(val eventId: String, val headers: Headers, val body: A) : SubscriptionMessage<A>()
data class EOSEvent(val statusCode: Int, val headers: Headers, val error: EosError) : SubscriptionMessage<Nothing>()
data class ControlEvent(val body: String) : SubscriptionMessage<Nothing>()

private fun <A> Array<JsonElement>.createControlEvent(): Result<SubscriptionMessage<A>, Error> =
    getOrElse(1) { JsonPrimitive("") }.asString.let { ControlEvent(body = it) }.asSuccess()

private fun <A> Array<JsonElement>.createEosEvent(): Result<SubscriptionMessage<A>, Error> =
    validate(statusCode, eosError) { statusCode, error ->
        EOSEvent(
            statusCode = statusCode,
            headers = headers,
            error = error
        )
    }

private fun <A> Array<JsonElement>.createSubscriptionEvent(
    typeResolver: SubscriptionTypeResolver
): Result<SubscriptionMessage<A>, Error> =
    validate(eventId, messageBody(typeResolver)) { eventId, body: A ->
        SubscriptionEvent(eventId, headers, body)
    }

private val Array<JsonElement>.statusCode: Result<Int, Error>
    get() = valueAt(1, "statusCode") { asInt }

private val Array<JsonElement>.eosError: Result<EosError, Error>
    get() = valueAt(3, "error")

private val Array<JsonElement>.headers: Headers
    get() = valueAt<Headers>(2, "headers").recover { emptyHeaders() }

private val Array<JsonElement>.eventId: Result<String, Error>
    get() = valueAt(1, "eventId")

private val Array<JsonElement>.messageType
    get() = valueAt<Int>(0, "messageType").recover { -1 }

private fun <A> Array<JsonElement>.messageBody(typeResolver: SubscriptionTypeResolver): Result<A, Error> =
    eventId.flatMap { id -> valueAt(3, "body") { asJsonObject }.flatMap { it.parseAs<A>(typeResolver(id)) } }

private fun <A, B, C> validate(a: Result<A, Error>, b: Result<B, Error>, block: (A, B) -> C): Result<C, Error> = when {
    a is Result.Success && b is Result.Success -> block(a.value, b.value).asSuccess()
    else -> Errors.compose(failuresOf(a, b)).asFailure()
}

private fun <A> Array<JsonElement>.valueAt(index: Int, name: String, block: JsonElement.() -> A): Result<A, Error> =
    getOrNull(index)?.block().orElse { Errors.other("Field '$name' not found in subscription message") }

private inline fun <reified A> Array<JsonElement>.valueAt(index: Int, name: String): Result<A, Error> =
    getOrNull(index)?.asJsonObject?.parseAs()
        ?: Errors.other("Field '$name' not found in subscription message").asFailure()

