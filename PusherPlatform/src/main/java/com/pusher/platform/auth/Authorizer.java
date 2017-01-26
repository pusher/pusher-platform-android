package com.pusher.platform.auth;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public interface Authorizer {
    void performRequest(Request request, Callback callback);
    void setHttpClient(OkHttpClient okHttpClient);
    void setUserId(String userId);
}
