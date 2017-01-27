package com.pusher.platform.auth;

import com.pusher.platform.App;
import com.pusher.platform.BaseClient;
import com.pusher.platform.subscription.ResumableSubscription;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * The magical unicorn that makes sure our authentication tokens are in order (Or ignored).
 * Intercepts requests from {@link ResumableSubscription} and {@link BaseClient}
 * */
public interface Authorizer {

    Call performRequest(Request request, Callback callback);

    /**
     * Set the OkHTTP client to the Authorizer. Must be called by {@link App}.
     * */
    void setHttpClient(OkHttpClient okHttpClient);

    /**
     * Set the current user ID. It should come from your existing authentication system.
     * */
    void setUserId(String userId);
}
