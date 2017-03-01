package com.pusher.platform.subscription;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.pusher.platform.auth.Authorizer;
import com.pusher.platform.logger.EmptyLogger;
import com.pusher.platform.logger.Logger;
import com.pusher.platform.subscription.event.MessageEvent;

import java.io.IOException;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.http2.StreamResetException;

public class ResumableSubscription {

    private final Logger logger;
    private Gson gson;
    private String lastEventId;
    private Request request;
    private OnOpenListener onOpenListener;
    private OnEventListener onEventListener;
    private SubscriptionErrorListener onErrorListener;
    private Authorizer authorizer;

    private Call subscribeCall;

    private ResumableSubscription(Authorizer authorizer, Logger logger, Request request) {
        this.gson = new Gson();
        this.authorizer= authorizer;
        this.logger = logger;
        this.request = request;
    }

    /**
     * Subscribe to events in this Resumable resource.
     * @param onOpenListener
     * @param onEventListener
     * @param onErrorListener
     * @param lastEventId
     * */
    public void subscribe(OnOpenListener onOpenListener, OnEventListener onEventListener, SubscriptionErrorListener onErrorListener, String lastEventId){

        this.onOpenListener = onOpenListener;

        if(null == onEventListener){
            throw new IllegalArgumentException("OnItemListener must not be null");
        }
        this.onEventListener = onEventListener;

        if(null == onErrorListener){
            throw new IllegalArgumentException("OnErrorListener must not be null");
        }
        this.onErrorListener = onErrorListener;

        if(lastEventId != null){
            this.lastEventId = lastEventId;
        }

        if(subscribeCall != null && subscribeCall.isExecuted()){
            throw new SubscriptionException(new IllegalStateException("Subscribe attempted twice"), SubscriptionException.Type.PEBKAC);
        }

        subscribe(this.lastEventId);
    }

    private void subscribe(String lastEventId){
        Request.Builder builder = request.newBuilder();
        if(lastEventId != null){
            builder.header("Last-Event-ID", lastEventId);
        }

        subscribeCall = authorizer.performRequest(builder.build(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                onErrorListener.onError(new SubscriptionException(e, SubscriptionException.Type.CONNECTION_LOST));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.code() == 200){
                    handleOpenSubscription(response);
                }
                else onErrorListener.onError(new SubscriptionException(new Throwable("REsponse to the subscription was not 200"), SubscriptionException.Type.NOT_200));
            }
        });
    }

    private void handleOpenSubscription(Response response) throws IOException {

        if(null != onOpenListener) onOpenListener.onOpen();

        try{
            while(!response.body().source().exhausted()){

                String messageString = response.body().source().readUtf8LineStrict();
                JsonElement[] message = gson.fromJson(messageString, JsonElement[].class);

                switch (message[0].getAsInt()){
                    case 0:
                        logger.log("0 event"); //Ignore
                        break;
                    case 1:
                        messageReceived(message);
                        break;

                    case 255:
                        response.close();
                        //TODO: onCloseListener.onClose();
                        break;

                    default:
                        subscribeCall.cancel();
                        subscribeCall = null;
                        onErrorListener.onError(new SubscriptionException(new IllegalStateException("Message is malformed"), SubscriptionException.Type.WTF_CONDITION));
                }
            }
        } catch (StreamResetException e){
            subscribeCall.cancel();
            subscribeCall = null;
             onErrorListener.onError(new SubscriptionException(e, SubscriptionException.Type.CONNECTION_LOST));
        }
    }

    private void messageReceived(JsonElement[] message) {
        MessageEvent messageEvent = new MessageEvent(message[1].getAsString(), gson.fromJson(message[2], Map.class), message[3]);
        onEventListener.onEvent(messageEvent);
        lastEventId = message[1].getAsString();
    }

    /**
     * Utility method to check whether the subscription is active.
     * */
    public boolean isSubscribed() {
        return (null != subscribeCall && subscribeCall.isExecuted() && !subscribeCall.isCanceled());
    }

    /**
     * Closes the currenct subscription, if it exists.
     * */
    public void close() {
        if(null != subscribeCall){
            subscribeCall.cancel();
        }
    }

    public static class Builder {

        private Request request;
        private Logger logger;
        private Authorizer authorizer;

        /**
         * An OkHTTP Request to subscribe with. Must not be null.
         * */
        public Builder request(Request request){
            this.request = request;
            return this;
        }

        public Builder logger(Logger logger){
            this.logger = logger;
            return this;
        }

        /**
         * The authorizer to authenticate the requests. Must not be null.
         * */
        public Builder authorizer(Authorizer authorizer){
            this.authorizer = authorizer;
            return this;
        }

        public ResumableSubscription build(){
            if(null == request){
                throw new IllegalStateException("Request must not be null");
            }
            if(null == authorizer){
                throw new IllegalStateException("Authorizer must not be null");
            }
            if(null == logger){
                logger = new EmptyLogger();
            }

            return new ResumableSubscription(authorizer, logger, request);
        }
    }
}

