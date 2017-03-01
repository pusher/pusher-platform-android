package com.pusher.platform;

/**
 * The main interface for when things don't go as planned.
 * */
public interface ErrorListener {
    void onError(Error error);
}
