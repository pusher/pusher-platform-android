package com.pusher.platform.feeds;

/**
 * Poor man's Retrofit
 * */
public interface DoneListener<T> {
    void onDone(T response);
}
