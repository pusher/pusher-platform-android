package com.pusher.platform.subscription;

import com.pusher.platform.ErrorListener;

public interface SubscriptionErrorListener extends ErrorListener {
    void onError(SubscriptionException exception);
}
