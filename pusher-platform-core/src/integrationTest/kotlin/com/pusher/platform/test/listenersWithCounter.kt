package com.pusher.platform.test

import com.pusher.platform.SubscriptionListeners
import elements.EOSEvent
import elements.Headers
import elements.SubscriptionEvent

/**
 * Counts interactions.
 */
data class SubscriptionInteractions(
    var open: Int = 0,
    var events: Int = 0,
    var end: Int = 0,
    var error: Int = 0,
    var retrying: Int = 0,
    var subscribe: Int = 0
)

private fun <A> printerListeners() = SubscriptionListeners<A>(
    onOpen = { println("onOpen($it)") },
    onEnd = { println("onEnd($it)") },
    onEvent = { println("onEvent($it)") },
    onError = { println("onError($it)") },
    onRetrying = { println("onRetrying()") },
    onSubscribe = { println("onSubscribe()") }
)

fun <A> listenersWithCounter(
    onOpen: SubscriptionInteractions.(headers: Headers) -> Unit = {},
    onEnd: SubscriptionInteractions.(error: EOSEvent?) -> Unit = {},
    onError: SubscriptionInteractions.(error: elements.Error) -> Unit = {},
    onEvent: SubscriptionInteractions.(event: SubscriptionEvent<A>) -> Unit = {},
    onRetrying: SubscriptionInteractions.() -> Unit = {},
    onSubscribe: SubscriptionInteractions.() -> Unit = {}
): SubscriptionListeners<A> {
    return SubscriptionListeners.compose(
        printerListeners(),
        with(SubscriptionInteractions()) {
            SubscriptionListeners<A>(
                onOpen = { onOpen(it) },
                onEnd = { onEnd(it) },
                onEvent = { onEvent(it) },
                onError = { onError(it) },
                onRetrying = { onRetrying() },
                onSubscribe = { onSubscribe() }
            )
        }
    )
}
