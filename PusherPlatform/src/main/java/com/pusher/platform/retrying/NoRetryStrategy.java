package com.pusher.platform.retrying;

/**
 * Empty implementation of RetryStrategy that immediately terminates
 * */
public class NoRetryStrategy implements RetryStrategy {
    @Override
    public void tryAgain(Callback callback) {
        callback.terminate(new Throwable("No Retry Strategy Specified"));
    }

    @Override
    public void cancel() {}
}
