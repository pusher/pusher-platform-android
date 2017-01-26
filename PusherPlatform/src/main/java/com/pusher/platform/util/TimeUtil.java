package com.pusher.platform.util;

import java.util.Date;

public class TimeUtil {
    public Date dateFromString(String dateString) {

        Date date = new Date(Long.parseLong(dateString) * 1000);

        return date;
    }

    public Date now() {
        return new Date();
    }

    public String dateFromSecondsInTheFuture(long expiresIn) {

        long nowSeconds = now().getTime()/1000;

        return String.valueOf(nowSeconds + expiresIn);
    }
}
