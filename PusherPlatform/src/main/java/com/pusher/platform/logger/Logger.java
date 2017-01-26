package com.pusher.platform.logger;

public interface Logger {
    void log(String message);
    void log(Throwable e, String message);
    void log(String message, Object... arguments);
    void log(Throwable e, String message, Object... arguments);
    void log(Object object);
}
