package com.pusher.platform.subscription;


public class SubscriptionException  extends IllegalStateException {
    public final Type type;

    public SubscriptionException(Throwable throwable, Type type ){
        super(throwable);
        this.type = type;
    }

    public enum Type {
        CONNECTION_LOST, WTF_CONDITION, PEBKAC, NOT_200
    }
}
