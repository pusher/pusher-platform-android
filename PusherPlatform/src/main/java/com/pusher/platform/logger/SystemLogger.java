package com.pusher.platform.logger;

public class SystemLogger implements Logger {

    @Override
    public void log(String message) {
        System.out.println(message);
    }

    @Override
    public void log(Throwable e, String message) {
        e.printStackTrace();
        log(message);
    }

    @Override
    public void log(String message, Object... arguments) {
        log(String.format(message, arguments));
    }

    @Override
    public void log(Throwable e, String message, Object... arguments) {
        e.printStackTrace();
        log(message, arguments);
    }

    @Override
    public void log(Object object) {
        log(object.toString());
    }
}
