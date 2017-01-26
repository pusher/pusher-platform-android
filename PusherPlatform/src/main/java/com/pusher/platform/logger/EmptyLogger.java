package com.pusher.platform.logger;

/**
 * Empty implementation of a logger for when we don't care about it
 * */
public class EmptyLogger implements Logger {
    @Override
    public void log(String message) {}

    @Override
    public void log(Throwable e, String message) {}

    @Override
    public void log(String message, Object... arguments) {}

    @Override
    public void log(Throwable e, String message, Object... arguments) {}

    @Override
    public void log(Object object) {}
}
