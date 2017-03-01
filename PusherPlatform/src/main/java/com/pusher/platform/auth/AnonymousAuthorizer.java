package com.pusher.platform.auth;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class AnonymousAuthorizer implements Authorizer {
    private OkHttpClient httpClient;

    public AnonymousAuthorizer(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public Call performRequest(Request request, Callback callback) {

        Call call = httpClient.newCall(request);
        call.enqueue(callback);
        return call;
    }

    @Override
    public void setHttpClient(OkHttpClient okHttpClient) {
        this.httpClient = okHttpClient;
    }

    @Override
    public void setUserId(String userId) { /* empty implementation */ }

    public static class Builder {
        private OkHttpClient httpClient;

        public Builder httpClient(OkHttpClient client){
            this.httpClient = client;
            return this;
        }

        public AnonymousAuthorizer build(){
            return new AnonymousAuthorizer(httpClient);
        }
    }
}
