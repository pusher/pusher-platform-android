package com.pusher.platform.logger;

/**
 * You have 3 guesses to figure out what this does.
 * */
public interface Logger {
    void log(String message);
    void log(Throwable e, String message);
    void log(String message, Object... arguments);
    void log(Throwable e, String message, Object... arguments);
    void log(Object object);
}
