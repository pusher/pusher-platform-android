package elements

import com.google.gson.JsonElement
import com.pusher.platform.BaseClient
import com.pusher.platform.subscription.BaseSubscription

sealed class SubscriptionMessage {
    companion object Factory {
        fun fromRaw(messageString: String): SubscriptionMessage {
            val message = BaseClient.GSON.fromJson(messageString, Array<JsonElement>::class.java)
            return when (message[0].asInt) {
                0 -> ControlEvent(body = message[1].asString)
                1 -> SubscriptionEvent(eventId = message[1].asString, headers = BaseClient.GSON.fromJson(message[2], Map::class.java) as Headers, body = message[3])
                255 -> EOSEvent(statusCode = message[1].asInt, headers = message[2] as Headers, errorBody = message[3])
                else -> throw kotlin.Error("Fuck $messageString") //TODO
            }
        }
    }
}
data class SubscriptionEvent(val messageType: Int = 1, val eventId: String, val headers: Headers, val body: JsonElement): SubscriptionMessage()
data class EOSEvent(val messageType: Int = 255, val statusCode: Int, val headers: Headers, val errorBody: JsonElement): SubscriptionMessage()
data class ControlEvent(val messageType: Int = 0, val body: String): SubscriptionMessage()
