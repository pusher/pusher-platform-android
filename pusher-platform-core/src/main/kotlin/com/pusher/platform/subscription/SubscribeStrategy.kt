package com.pusher.platform.subscription

import com.pusher.platform.SubscriptionListeners
import elements.Headers
import elements.Subscription

internal typealias SubscribeStrategy<A> = (listeners: SubscriptionListeners<A>, headers: Headers) -> Subscription
