package com.pusher.platform.metrics;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Adds the Pusher-Client header to outgoing network requests.
 * */
public class ClientHeaderInjectingInterceptor implements Interceptor {

    private String trackingString;

    public ClientHeaderInjectingInterceptor(String libraryName, String clientVersion, String osVersion, String deviceId){

        HttpUrl url = new HttpUrl.Builder().scheme("http").host("pusher.com")
                .addQueryParameter("client", libraryName)
                .addQueryParameter("client-version", clientVersion)
                .addQueryParameter("os", osVersion)
                .addQueryParameter("device", deviceId)
                .build();

        //A filthy hack and abuse of HttpUrl
        trackingString = url.encodedQuery();
    }

    private static final String PUSHER_CLIENT = "Pusher-Client";

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request().newBuilder()
                .addHeader(PUSHER_CLIENT, trackingString)
                .build();
        return chain.proceed(request);
    }
}
