package elements

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.pusher.util.ResultAssertions.assertSuccess
import com.pusher.util.ResultAssertions.assertFailure
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

class SubscriptionMessageTest : Spek({

    describe("any event fails") {

        it("with empty array") {
            val result = SubscriptionMessage.fromRaw("[]")

            assertFailure(result).isEqualTo(Errors.other("Unknown message type: []"))
        }

        it("with incorrect message type") {
            val result = SubscriptionMessage.fromRaw("[\"not a message type\"]")

            assertFailure(result).isEqualTo(Errors.other("Unknown message type: [\"not a message type\"]"))
        }

        it("with empty content") {
            val result = SubscriptionMessage.fromRaw("")

            assertFailure(result).isEqualTo(Errors.other("Unknown message type: "))
        }

    }

    describe("control event parses successfully") {

        it("with body") {
            val result = SubscriptionMessage.fromRaw("[0, \"body\"]")

            assertSuccess(result).isEqualTo(ControlEvent("body"))
        }

        it("without body") {
            val result = SubscriptionMessage.fromRaw("[0]")

            assertSuccess(result).isEqualTo(ControlEvent(""))
        }

        it("with extra values") {
            val result = SubscriptionMessage.fromRaw("[0, \"body\", \"this is fine\"]")

            assertSuccess(result).isEqualTo(ControlEvent("body"))
        }

    }

    describe("subscription event parses successfully") {

        it("without content") {
            val result = SubscriptionMessage.fromRaw("[1, \"id\", {}, {}]")

            assertSuccess(result).isEqualTo(SubscriptionEvent("id", emptyMap(), JsonObject()))
        }

        it("with headers") {
            val result = SubscriptionMessage.fromRaw("[1, \"id\", {\"header\": [\"value\"]}, {}]")

            assertSuccess(result).isEqualTo(SubscriptionEvent("id", mapOf("header" to listOf("value")), JsonObject()))
        }

        it("with body") {
            val result = SubscriptionMessage.fromRaw("[1, \"id\", {}, {\"key\": \"value\"}]")

            assertSuccess(result).isEqualTo(SubscriptionEvent("id", emptyMap(), JsonObject().apply {
                add("key", JsonPrimitive("value"))
            }))
        }

    }

    describe("subscription event fails to parse") {

        it("without body") {
            val result = SubscriptionMessage.fromRaw("[1, \"id\", {}]")

            assertFailure(result).isEqualTo(Errors.compose(Errors.other("Field 'body' not found in subscription message")))
        }

        it("without body or headers") {
            val result = SubscriptionMessage.fromRaw("[1, \"id\"]")

            assertFailure(result).isEqualTo(Errors.compose(
                Errors.other("Field 'body' not found in subscription message")
            ))
        }

        it("without body, headers or eventId") {
            val result = SubscriptionMessage.fromRaw("[1]")

            assertFailure(result).isEqualTo(Errors.compose(
                Errors.other("Field 'eventId' not found in subscription message"),
                Errors.other("Field 'body' not found in subscription message")
            ))
        }

    }

    describe("Eos event parses successfully") {

        it("without content") {
            val result = SubscriptionMessage.fromRaw("[255, 9000, {}, {}]")

            assertSuccess(result).isEqualTo(EOSEvent(9000, emptyMap(), JsonObject()))
        }

        it("with headers") {
            val result = SubscriptionMessage.fromRaw("[255, 9000, {\"header\": [\"value\"]}, {}]")

            assertSuccess(result).isEqualTo(EOSEvent(9000, mapOf("header" to listOf("value")), JsonObject()))
        }

        it("with body") {
            val result = SubscriptionMessage.fromRaw("[255, 9000, {}, {\"key\": \"value\"}]")

            assertSuccess(result).isEqualTo(EOSEvent(9000, emptyMap(), JsonObject().apply {
                add("key", JsonPrimitive("value"))
            }))
        }

    }

    describe("Eos event fails to parse") {

        it("without errorBody") {
            val result = SubscriptionMessage.fromRaw("[255, 9000, {}]")

            assertFailure(result).isEqualTo(Errors.compose(Errors.other("Field 'errorBody' not found in subscription message")))
        }

        it("without body or headers") {
            val result = SubscriptionMessage.fromRaw("[255, 9000]")

            assertFailure(result).isEqualTo(Errors.compose(
                Errors.other("Field 'errorBody' not found in subscription message")
            ))
        }

        it("without body, headers or eventId") {
            val result = SubscriptionMessage.fromRaw("[255]")

            assertFailure(result).isEqualTo(Errors.compose(
                Errors.other("Field 'statusCode' not found in subscription message"),
                Errors.other("Field 'errorBody' not found in subscription message")
            ))
        }

    }

})
