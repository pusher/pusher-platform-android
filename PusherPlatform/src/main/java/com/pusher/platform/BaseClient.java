package com.pusher.platform;

import com.google.gson.Gson;
import com.pusher.platform.auth.Authorizer;
import com.pusher.platform.logger.EmptyLogger;
import com.pusher.platform.logger.Logger;
import com.pusher.platform.retrying.RetryStrategy;
import com.pusher.platform.subscription.OnErrorListener;
import com.pusher.platform.subscription.OnEventListener;
import com.pusher.platform.subscription.OnOpenListener;
import com.pusher.platform.subscription.ResumableSubscription;
import com.pusher.platform.subscription.SubscriptionException;

import java.io.IOException;

import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Creates requests and relays to the {@link Authorizer} to be executed. Relays any responses or errors back to the caller.
 * */
public class BaseClient {

    private Logger logger;
    private Gson gson;
    private Authorizer authorizer;

    private BaseClient(Authorizer authorizer, Logger logger) {
        this.logger = logger;
        this.authorizer = authorizer;
        this.gson = new Gson(); //TODO: this is not used
    }

    public void request(String method, String url, Headers headers, String body, final Callback callback) {
        RequestBody requestBody = null;
        if (body != null) {
            requestBody = RequestBody.create(MediaType.parse("application/json"), body);
        }

        headers = headers.newBuilder()
                .add("Pusher-Client-Version", "ClientVersionBlaBlaBla") //TODO: versioning
                .build();

        Request request = new Request.Builder()
                .method(method.toUpperCase(), requestBody)
                .url(url)
                .headers(headers)
                .cacheControl(new CacheControl.Builder().noCache().noStore().build())
                .build();

        authorizer.performRequest(request, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

                callback.onFailure(call, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                callback.onResponse(call, response);
            }
        });
    }

    /**
     * Performs any HTTP request we use in Pusher Platform. It's a thin layer above OKHTTP and adds Pusher specific headers and similar
     * */
    public void request(String method, String url, Headers headers, final Callback callback){
        request(method, url, headers, null, callback);
    }

    class RetryingErrorHandler implements OnErrorListener {

        private final RetryStrategy retryStrategy;
        private final ResumableSubscription subscription;
        private final OnOpenListener onOpen;
        private final OnEventListener onEventListener;
        private final OnErrorListener onError;

        RetryingErrorHandler(RetryStrategy retryStrategy, ResumableSubscription subscription, OnOpenListener onOpen, OnEventListener onEventListener, OnErrorListener onError){
            this.retryStrategy = retryStrategy;
            this.subscription = subscription;
            this.onOpen = onOpen;
            this.onEventListener = onEventListener;
            this.onError = onError;
        }

        @Override
        public void onError(SubscriptionException exception) {
            retryStrategy.tryAgain(new RetryStrategy.Callback() {
                @Override
                public void retryNow() {
                    logger.log("retryNow");
                    subscription.subscribe(onOpen, onEventListener, RetryingErrorHandler.this, null);
                }

                @Override
                public void terminate(Throwable t) {
                    logger.log(t, "terminate");
                    onError.onError(new SubscriptionException(t, SubscriptionException.Type.WTF_CONDITION));
                }
            });
        }
    }

    public ResumableSubscription subscribe(String url, final OnOpenListener onOpen, final OnEventListener onEventListener, final OnErrorListener onError, final String lastItemId, final RetryStrategy retryStrategy) {

        final ResumableSubscription subscription = createSubscription(url);

        OnErrorListener retryingErrorHandler = new RetryingErrorHandler(
                retryStrategy, subscription, onOpen, onEventListener, onError
        );

        subscription.subscribe(onOpen, onEventListener, retryingErrorHandler, lastItemId);
        return subscription;
    }

    private ResumableSubscription createSubscription(String url){

        //TODO: hook the authorizer into the subscription. Lol.
        logger.log("createSubscription: %s", url);

        ResumableSubscription subscription;
        Request httpRequest = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .method("SUBSCRIBE", null)
                .cacheControl(new CacheControl.Builder().noCache().noStore().build())
                .build();

        subscription = new ResumableSubscription.Builder()
                .logger(logger)
                .request(httpRequest)
                .authorizer(authorizer)
                .build();

        return subscription;
    }

    public static class Builder {

        private Logger logger;
        private Authorizer authorizer;

        public Builder logger(Logger logger){
            this.logger = logger;
            return this;
        }

        /**
         * Sets the authorizer. This call is mandatory.
         * */
        public Builder authorizer(Authorizer authorizer){
            this.authorizer = authorizer;
            return this;
        }

        public BaseClient build(){

            if(null == logger){
                logger = new EmptyLogger();
            }
            if(null == authorizer) throw new IllegalStateException("Authorizer must not be null");
            BaseClient client = new BaseClient(authorizer, logger);

            return client;
        }
    }
}
