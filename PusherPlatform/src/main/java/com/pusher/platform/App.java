package com.pusher.platform;

import android.content.Context;
import android.os.Build;

import com.pusher.platform.auth.AnonymousAuthorizer;
import com.pusher.platform.auth.Authorizer;
import com.pusher.platform.auth.SharedPreferencesAuthorizer;
import com.pusher.platform.logger.EmptyLogger;
import com.pusher.platform.logger.Logger;
import com.pusher.platform.logger.SystemLogger;
import com.pusher.platform.metrics.ClientHeaderInjectingInterceptor;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * This is the main point for configuration and interacting with services built on top of Pusher Platform.
 * It encapsulates the connection, requests via {@link BaseClient}, authentication logic via {@link Authorizer}, and logging via {@link Logger}.
 * To use it, pass it to the services you use.
 *
 * */
public class App {

    public final String cluster;
    public final String id;
    public final BaseClient client;
    public final Logger logger;
    public final Authorizer authorizer;
    private String userId;

    private App(String id, String cluster, BaseClient client, Logger logger, Authorizer authorizer) {
        this.client = client;
        this.id = id;
        this.cluster = cluster;
        this.logger = logger;
        this.authorizer = authorizer;
    }

    public BaseClient getClient() {
        return client;
    }

    public String baseUrl() {
        return String.format("https://api.private-beta-1.pusherplatform.com:443/apps/%s", id);
    }

    /**
     * When authenticated by your backend of choice, pass your user ID in order to make authenticated requests as this user.
     * This relays the user ID to the underlying authorizer
     * */
    public void setUserId(String userId) {
        this.userId = userId;
        authorizer.setUserId(userId);
    }

    public static class Builder {
        private String id;
        private String cluster;
        private OkHttpClient okHttpClient;
        private BaseClient baseClient;
        private Logger logger;
        private Authorizer authorizer;

        /**
         *
         * Sets the ID of the application - you get that from your Pusher Platform dashboard. This field is mandatory.
         * */
        public Builder id(String id){
            this.id = id;
            return this;
        }

        /**
         * Application-wide cluster - you get that from your Pusher Platform dashboard
         *
         * */
        public Builder cluster(String cluster){
            this.cluster = cluster;
            return this;
        }

        public Builder httpClient(OkHttpClient client){
            this.okHttpClient = client;
            return this;
        }

        /**
         * Sets the logger. Provided implementations are {@link SystemLogger} or {@link EmptyLogger} (default) for no logging.
         * */
        public Builder logger(Logger logger){
            this.logger = logger;
            return this;
        }

        /**
         * Sets the Authorizer. Use this in order to make non-anonymous requests. Provided implementations are {@link SharedPreferencesAuthorizer} or {@link AnonymousAuthorizer} (default).
         * */
        public Builder authorizer(Authorizer authorizer){
            this.authorizer = authorizer;
            return this;
        }

        public App build(){

            if(id == null){
                throw new IllegalStateException("App ID not Set");
            }

            String libraryName = "pusher-platform-android";
            String libraryVersion = "0.2.0";
            String os = Build.VERSION.RELEASE;
            String device = Build.DEVICE;
            ClientHeaderInjectingInterceptor interceptor = new ClientHeaderInjectingInterceptor(libraryName, libraryVersion, os, device);

            OkHttpClient.Builder httpClientBuilder;
            if(null == okHttpClient ) {
                httpClientBuilder = new OkHttpClient.Builder();

            }else{
                httpClientBuilder = okHttpClient.newBuilder();
            }
            okHttpClient = httpClientBuilder
                    .connectTimeout(10000, TimeUnit.MINUTES)
                    .writeTimeout(10000, TimeUnit.MINUTES)
                    .readTimeout(10000, TimeUnit.MINUTES)
                    .addNetworkInterceptor(interceptor)
                    .build();

            if(authorizer == null) authorizer = new AnonymousAuthorizer(okHttpClient);
            else{
                authorizer.setHttpClient(okHttpClient);
            }

            baseClient = new BaseClient.Builder()
                    .logger(logger)
                    .authorizer(authorizer)
                    .build();

            return new App(
                    id,
                    cluster != null ? cluster: "beta",
                    baseClient,
                    logger != null ? logger: new EmptyLogger(),
                    authorizer);
        }
    }
}
