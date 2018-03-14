package elements

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.pusher.platform.network.parseAs
import com.pusher.util.Result
import com.pusher.util.Result.Companion.failuresOf
import com.pusher.util.asFailure
import com.pusher.util.asSuccess
import com.pusher.util.orElse

sealed class SubscriptionMessage {
    companion object Factory {
        fun fromRaw(messageString: String): Result<SubscriptionMessage, Error> =
            messageString.parseAs<Array<JsonElement>>().flatMap {
                when (it.messageType) {
                    0 -> it.createControlEvent()
                    1 -> it.createSubscriptionEvent()
                    255 -> it.createEosEvent()
                    else -> Errors.other("Unknown message type: $messageString").asFailure()
                }
            }
    }
}

private val Array<JsonElement>.messageType
    get() = getOrNull(0)?.asInt ?: -1

private fun <A> Array<JsonElement>.valueAt(index: Int, name: String, block: JsonElement.() -> A): Result<A, Error> =
    getOrNull(index)?.block().orElse { Errors.other("Field '$name' of type Int not found in subscription message") }

private fun Array<JsonElement>.createControlEvent(): Result<SubscriptionMessage, Error> =
    valueAt(0, "body") { asString }.map { ControlEvent(body = it) }

private fun Array<JsonElement>.createEosEvent(): Result<SubscriptionMessage, Error> {
    val statusCode = valueAt(1, "statusCode") { asInt }
    val headers = valueAt(2, "headers") { asJsonObject }.flatMap { it.parseAs<Headers>() }
    val errorBody = valueAt(3, "errorBody") { asJsonObject }

    return checkProperties(statusCode, errorBody)
        ?: EOSEvent(
            statusCode = statusCode.recover { -1 },
            headers = headers.recover { emptyMap() },
            errorBody = errorBody.recover { JsonObject() }
        ).asSuccess<SubscriptionMessage, Error>()
}

private fun Array<JsonElement>.createSubscriptionEvent(): Result<SubscriptionMessage, Error> {
    val eventId = valueAt(1, "eventId") { asString }
    val headers = valueAt(2, "headers") { asJsonObject }.flatMap { it.parseAs<Headers>() }
    val body = valueAt(3, "body") { asJsonObject }

    return checkProperties(eventId, body)
        ?: SubscriptionEvent(
            eventId = eventId.recover { "error" },
            headers = headers.recover { emptyMap() },
            body = body.recover { JsonObject() }
        ).asSuccess<SubscriptionMessage, Error>()
}

private fun checkProperties(vararg results: Result<*, Error>) =
    failuresOf(*results)
        .takeIf { it.isNotEmpty() }
        ?.let { Errors.compose(it).asFailure<SubscriptionMessage, Error>() }

data class SubscriptionEvent(val messageType: Int = 1, val eventId: String, val headers: Headers, val body: JsonElement) : SubscriptionMessage()
data class EOSEvent(val messageType: Int = 255, val statusCode: Int, val headers: Headers, val errorBody: JsonElement) : SubscriptionMessage()
data class ControlEvent(val messageType: Int = 0, val body: String) : SubscriptionMessage()
