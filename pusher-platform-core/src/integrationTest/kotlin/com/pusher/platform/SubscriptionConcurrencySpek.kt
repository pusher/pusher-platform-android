package com.pusher.platform

import com.google.gson.JsonElement
import com.pusher.platform.network.DataParser
import com.pusher.platform.network.parseAs
import com.pusher.platform.retrying.RetryStrategyOptions
import com.pusher.platform.test.describeWhenReachable
import com.pusher.platform.test.listenersWithCounter
import com.pusher.platform.test.will
import elements.Subscription
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.Assert
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.atomic.AtomicInteger

class SubscriptionConcurrencySpek : Spek({
    val pathForever = "subscribe_forever"
    val bodyParser: DataParser<JsonElement> = { it.parseAs<JsonElement>() }

    describe("Instance Subscribe") {
        val instance = Instance(
                locator = "v1:api-ceres:test",
                serviceName = "platform_sdk_tester",
                serviceVersion = "v1",
                dependencies = TestDependencies(),
                baseClient = insecureHttpClient
        )

        it("should cancel subscriptions accurately from lots of threads") {
            val numSubs = 50
            val outstanding = AtomicInteger(numSubs)
            val events = AtomicInteger(0)
            val queue = LinkedBlockingDeque<Subscription>()
            val producer = Thread() {
                for (i in 1..numSubs) {
                    val sub = instance.subscribeNonResuming(
                            path = pathForever,
                            retryOptions = RetryStrategyOptions(limit = 0),
                            messageParser = bodyParser,
                            listeners = listenersWithCounter(
                                    onEvent = {
                                        events.incrementAndGet()
                                    },
                                    onEnd = {
                                        outstanding.decrementAndGet()
                                    },
                                    onError = { Assert.fail("We should not get an error") }
                            )
                    )
                    queue.add(sub)
                }
            }
            producer.run()

            for (i in 1..5) {
                val consumer = Thread() {
                    val sub = queue.take()
                    sub.unsubscribe()
                }
                consumer.run()
            }

            while (outstanding.get() > 0) {
                Thread.sleep(10)
                println("TOTAL REMAIN ")
            }
            val eventsDuringSubs = events.get()
            println("got $eventsDuringSubs events while subscribed")

            Thread.sleep(1000)

            val eventsAtEnd =events.get()
            println("got $eventsAtEnd events at end")
            Assert.assertEquals(eventsAtEnd, eventsDuringSubs)
        }
    }
})