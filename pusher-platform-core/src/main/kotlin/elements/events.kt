package elements

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.pusher.platform.network.parseAs
import com.pusher.platform.subscription.SubscriptionTypeResolver
import com.pusher.util.Result
import com.pusher.util.Result.Companion.failuresOf
import com.pusher.util.asFailure
import com.pusher.util.asSuccess
import com.pusher.util.orElse

internal fun <A> String.toSubscriptionMessage(
    typeResolver: SubscriptionTypeResolver
): Result<SubscriptionMessage<A>, Error> =
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
    validate(statusCode) { statusCode ->
        EOSEvent(
            statusCode = statusCode,
            headers = headers,
            error = eosError
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

private val Array<JsonElement>.eosError: EosError
    get() = valueAt(3, "error") {
        asJsonObject.apply {
            if (!has("type")) addProperty("type", "unknown")
            if (!has("reason")) addProperty("reason", "unknown")
        }
    }
        .flatMap { it.parseAs<EosError>() }
        .recover { EosError("unknown", "unknown") }

private val Array<JsonElement>.headers: Headers
    get() = valueAt<Headers>(2, "headers").recover { emptyHeaders() }

private val Array<JsonElement>.eventId: Result<String, Error>
    get() = valueAt(1, "eventId")

private val Array<JsonElement>.messageType
    get() = valueAt(0, "messageType") { asJsonPrimitive.takeIf { it.isNumber }?.asInt }.recover { -1 }

private fun <A> Array<JsonElement>.messageBody(
    typeResolver: SubscriptionTypeResolver
): Result<A, Error> = eventId
    .flatRecover { fieldNotFoundError("body").asFailure() }
    .flatMap { id -> jsonBody.parseAs<A>(typeResolver(id)) }

private val Array<JsonElement>.jsonBody
    get() = valueAt<JsonElement>(3, "body")

private fun <A, B, C> validate(a: Result<A, Error>, b: Result<B, Error>, block: (A, B) -> C): Result<C, Error> = when {
    a is Result.Success && b is Result.Success -> block(a.value, b.value).asSuccess()
    else -> Errors.compose(failuresOf(a, b)).asFailure()
}

private fun <A, C> validate(a: Result<A, Error>, block: (A) -> C): Result<C, Error> = when (a) {
    is Result.Success -> block(a.value).asSuccess()
    else -> Errors.compose(failuresOf(a)).asFailure()
}

private fun <A> Array<JsonElement>?.valueAt(index: Int, name: String, block: JsonElement.() -> A?): Result<A, Error> =
    this?.getOrNull(index)?.block().orElse { fieldNotFoundError(name) }

private inline fun <reified A> Array<JsonElement>.valueAt(index: Int, name: String): Result<A, Error> =
    getOrNull(index)?.parseAs()
        ?:fieldNotFoundError(name).asFailure()

private fun fieldNotFoundError(name: String) =
    Errors.other("Field '$name' not found in subscription message")

