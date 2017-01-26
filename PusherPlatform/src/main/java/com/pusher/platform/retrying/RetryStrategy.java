package com.pusher.platform.retrying;

public interface RetryStrategy {
    void tryAgain(Callback callback);
    void cancel();

    interface Callback{
        void retryNow();
        void terminate(Throwable t);
    }
}
