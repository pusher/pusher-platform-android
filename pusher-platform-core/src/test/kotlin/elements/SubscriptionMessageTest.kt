package elements

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.pusher.platform.network.parseAs
import com.pusher.util.ResultAssertions.assertFailure
import com.pusher.util.ResultAssertions.assertSuccess
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

class SubscriptionMessageTest : Spek({

    describe("any event fails") {

        it("with empty array") {
            val result = "[]".toJsonSubscriptionMessage()

            assertFailure(result).isEqualTo(Errors.other("Unknown message type: []"))
        }

        it("with incorrect message type") {
            val result = "[\"not a message type\"]".toJsonSubscriptionMessage()

            assertFailure(result).isEqualTo(Errors.other("Unknown message type: [\"not a message type\"]"))
        }

        it("with empty content") {
            val result = "".toJsonSubscriptionMessage()

            assertFailure(result).isEqualTo(Errors.other("Unknown message type: "))
        }

    }

    describe("control event parses successfully") {

        it("with body") {
            val result = "[0, \"body\"]".toJsonSubscriptionMessage()

            assertSuccess(result).isEqualTo(ControlEvent("body"))
        }

        it("without body") {
            val result = "[0]".toJsonSubscriptionMessage()

            assertSuccess(result).isEqualTo(ControlEvent(""))
        }

        it("with extra values") {
            val result = "[0, \"body\", \"this is fine\"]".toJsonSubscriptionMessage()

            assertSuccess(result).isEqualTo(ControlEvent("body"))
        }

    }

    describe("subscription event parses successfully") {

        it("without content") {
            val result = "[1, \"id\", {}, {}]".toJsonSubscriptionMessage()

            assertSuccess(result).isEqualTo(SubscriptionEvent("id", emptyMap(), JsonObject()))
        }

        it("with headers") {
            val result = "[1, \"id\", {\"header\": [\"value\"]}, {}]".toJsonSubscriptionMessage()

            assertSuccess(result).isEqualTo(SubscriptionEvent("id", mapOf("header" to listOf("value")), JsonObject()))
        }

        it("with body") {
            val result = "[1, \"id\", {}, {\"key\": \"value\"}]".toJsonSubscriptionMessage()

            assertSuccess(result).isEqualTo(SubscriptionEvent("id", emptyMap(), JsonObject().apply {
                add("key", JsonPrimitive("value"))
            }))
        }

    }

    describe("subscription event fails to parse") {

        it("without body") {
            val result = "[1, \"id\", {}]".toJsonSubscriptionMessage()

            assertFailure(result).isEqualTo(Errors.compose(Errors.other("Field 'body' not found in subscription message")))
        }

        it("without body or headers") {
            val result = "[1, \"id\"]".toJsonSubscriptionMessage()

            assertFailure(result).isEqualTo(Errors.compose(
                Errors.other("Field 'body' not found in subscription message")
            ))
        }

        it("without body, headers or eventId") {
            val result = "[1]".toJsonSubscriptionMessage()

            assertFailure(result).isEqualTo(Errors.compose(
                Errors.other("Field 'eventId' not found in subscription message"),
                Errors.other("Field 'body' not found in subscription message")
            ))
        }

    }

    describe("Eos event parses successfully") {

        it("without content") {
            val result = "[255, 9000, {}, {}]".toJsonSubscriptionMessage()

            assertSuccess(result).isEqualTo(EOSEvent(9000, emptyMap(), EosError("unknown", "unknown")))
        }

        it("with headers") {
            val result = "[255, 9000, {\"header\": [\"value\"]}, {}]".toJsonSubscriptionMessage()

            assertSuccess(result).isEqualTo(EOSEvent(9000, mapOf("header" to listOf("value")), EosError("unknown", "unknown")))
        }

        it("with body") {
            val result = "[255, 9000, {}, {\"type\": \"type\", \"reason\" : \"reason\"}]".toJsonSubscriptionMessage()

            assertSuccess(result).isEqualTo(EOSEvent(9000, emptyMap(), EosError("type", "reason")))
        }

    }

    describe("Eos event fails to parse") {

        it("without error") {
            val result = "[255, 9000, {}]".toJsonSubscriptionMessage()

            assertSuccess(result).isEqualTo(
                EOSEvent(9000, emptyHeaders(), EosError("unknown", "unknown"))
            )
        }

        it("without body or headers") {
            val result = "[255, 9000]".toJsonSubscriptionMessage()

            assertSuccess(result).isEqualTo(
                EOSEvent(9000, emptyHeaders(), EosError("unknown", "unknown"))
            )
        }

        it("without body, headers or eventId") {
            val result = "[255]".toJsonSubscriptionMessage()

            assertFailure(result).isEqualTo(Errors.compose(
                Errors.other("Field 'statusCode' not found in subscription message")
            ))
        }

    }

})

private fun String.toJsonSubscriptionMessage() =
    toSubscriptionMessage { it.parseAs<JsonElement>() }
