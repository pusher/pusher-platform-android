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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit
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
            val numSubs = 10000
            val outstanding = AtomicInteger(numSubs)
            val events = AtomicInteger(0)
            val queue = LinkedBlockingDeque<Subscription>()
            val producer = Thread {
                for (i in 1..numSubs) {
                    Thread.sleep(5)
                    val sub = instance.subscribeNonResuming(
                            path = pathForever,
                            retryOptions = RetryStrategyOptions(limit = 0),
                            messageParser = bodyParser,
                            listeners = SubscriptionListeners(
                                    onEvent = {
                                        events.incrementAndGet()
                                    },
                                    onEnd = {
                                        outstanding.decrementAndGet()
                                    },
                                    onError = { Assert.fail("We should not get an error") }
                            )
                    )
                    queue.offer(sub)
                }
            }
            producer.start()

            val numConsumers = 20
            val latch = CountDownLatch(numConsumers)
            for (i in 1..numConsumers) {
                val consumer = Thread {
                    try {
                        while (true) {
                            val sub = queue.poll(5, TimeUnit.SECONDS)
                            sub ?: break
                            Thread.sleep(Math.round(Math.random() * 200))
                            sub.unsubscribe()
                        }
                    } finally {
                        println("CONSUMER EXITED")
                        latch.countDown()
                    }
                }
                consumer.start()
            }

            while (!latch.await(1, TimeUnit.SECONDS)) {
                val remaining = outstanding.get()
                println("TOTAL REMAINING $remaining")
            }

            val eventsDuringSubs = events.get()
            println("got $eventsDuringSubs events while subscribed")

            Thread.sleep(1000)

            val eventsAtEnd = events.get()
            println("got $eventsAtEnd events at end")
            Assert.assertEquals(eventsAtEnd, eventsDuringSubs)
        }
    }
})