package com.pusher.platform.auth;

import okhttp3.Request;

public interface AuthTokenCallback {
    void tokenReceived(Request request);
    void tokenError(TokenError error);
}
