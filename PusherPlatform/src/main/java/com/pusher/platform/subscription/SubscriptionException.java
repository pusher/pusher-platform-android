package com.pusher.platform.subscription;

import com.pusher.platform.Error;

/**
 * Thrown when something bad happens to your subscription
 * See {@link Type} for more explanation
 * /TODO: this might be ditched
 * */
public class SubscriptionException  extends Error {
    public final Type type;

    public SubscriptionException(Throwable throwable, Type type ){
        super(throwable);
        this.type = type;
    }

    public enum Type {
        CONNECTION_LOST,
        WTF_CONDITION,
        PEBKAC,
        NOT_200
    }
}
