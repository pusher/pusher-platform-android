package com.pusher.platform.retrying;

/**
 * The logic for retrying calls and subscriptions goes here.
 * */
public interface RetryStrategy {
    /**
     * Try making the network call again.
     * */
    void tryAgain(Callback callback);

    /**
     * Give up and stop trying.
     * */
    void cancel();

    interface Callback{
        void retryNow();
        void terminate(Throwable t);
    }
}
