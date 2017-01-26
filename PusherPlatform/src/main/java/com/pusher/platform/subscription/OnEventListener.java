package com.pusher.platform.subscription;

import com.pusher.platform.subscription.event.Event;

public interface OnEventListener {
    void onEvent(Event event);
}
