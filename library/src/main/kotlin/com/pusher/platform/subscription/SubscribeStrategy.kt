package com.pusher.platform.subscription

import elements.Subscription
import com.pusher.platform.SubscriptionListeners
import elements.Headers

typealias SubscribeStrategy = (listeners: SubscriptionListeners, headers: Headers) -> Subscription