package elements

import com.google.gson.JsonElement
import com.pusher.platform.network.parseAs
import com.pusher.platform.network.parseOr

sealed class SubscriptionMessage {
    companion object Factory {
        fun fromRaw(messageString: String): SubscriptionMessage {
            val message = messageString.parseAs<Array<JsonElement>>()
            return when (message[0].asInt) {
                0 -> ControlEvent(body = message[1].asString)
                1 -> SubscriptionEvent(eventId = message[1].asString, headers = message[2].parseOr { emptyMap<String, List<String>>() }, body = message[3])
                255 -> EOSEvent(statusCode = message[1].asInt, headers = message[2].parseOr { emptyMap<String, List<String>>() }, errorBody = message[3])
                else -> throw kotlin.Error("Unknown message type: $messageString") // TODO: Handle more gracefully
            }
        }
    }
}
data class SubscriptionEvent(val messageType: Int = 1, val eventId: String, val headers: Headers, val body: JsonElement): SubscriptionMessage()
data class EOSEvent(val messageType: Int = 255, val statusCode: Int, val headers: Headers, val errorBody: JsonElement): SubscriptionMessage()
data class ControlEvent(val messageType: Int = 0, val body: String): SubscriptionMessage()
