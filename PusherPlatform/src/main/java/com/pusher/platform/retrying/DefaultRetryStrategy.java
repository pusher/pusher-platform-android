package com.pusher.platform.retrying;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DefaultRetryStrategy implements RetryStrategy {

    private static final int MAX_RECONNECTION_ATTEMPTS = 12;
    private static final int MAX_RECONNECT_GAP_IN_SECONDS = 80;
    private int reconnectAttempts = 0;

    ScheduledFuture scheduledFuture;

    @Override
    public void tryAgain(final Callback callback) {

        if(reconnectAttempts < MAX_RECONNECTION_ATTEMPTS){
            reconnectAttempts++;
            long reconnectInterval = Math.min(MAX_RECONNECT_GAP_IN_SECONDS, reconnectAttempts * reconnectAttempts);

            scheduledFuture = Executors.newSingleThreadScheduledExecutor().schedule(new Runnable() {
                @Override
                public void run() {
                    callback.retryNow();
                }
            }, reconnectInterval, TimeUnit.SECONDS);
        }
        else{
            callback.terminate(new IllegalStateException("Too many reconnects"));
        }
    }

    @Override
    public void cancel() {
        if(null != scheduledFuture) scheduledFuture.cancel(true);
    }
}
