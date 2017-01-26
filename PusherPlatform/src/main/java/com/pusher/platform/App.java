package com.pusher.platform;

import com.pusher.platform.auth.AnonymousAuthorizer;
import com.pusher.platform.auth.Authorizer;
import com.pusher.platform.logger.EmptyLogger;
import com.pusher.platform.logger.Logger;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

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


        public Builder logger(Logger logger){
            this.logger = logger;
            return this;
        }

        public Builder authorizer(Authorizer authorizer){
            this.authorizer = authorizer;
            return this;
        }

        public App build(){

            if(id == null){
                throw new IllegalStateException("App ID not Set");
            }

            //TODO: due to very high timeouts we shouldn't reuse users OkHttpClients. We should instead clone them and reuse then.
            if(null == okHttpClient ) {
                okHttpClient = new OkHttpClient.Builder()
                        .connectTimeout(10000, TimeUnit.MINUTES)
                        .writeTimeout(10000, TimeUnit.MINUTES)
                        .readTimeout(10000, TimeUnit.MINUTES)
                        .build();
            }

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
