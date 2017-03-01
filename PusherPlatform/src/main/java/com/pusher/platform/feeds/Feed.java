package com.pusher.platform.feeds;

import android.util.Log;

import com.google.gson.Gson;
import com.pusher.platform.App;
import com.pusher.platform.BaseClient;
import com.pusher.platform.Error;
import com.pusher.platform.ErrorListener;
import com.pusher.platform.retrying.DefaultRetryStrategy;
import com.pusher.platform.retrying.NoRetryStrategy;
import com.pusher.platform.retrying.RetryStrategy;
import com.pusher.platform.subscription.OnEventListener;
import com.pusher.platform.subscription.OnOpenListener;
import com.pusher.platform.subscription.ResumableSubscription;
import com.pusher.platform.subscription.event.Event;
import com.pusher.platform.subscription.event.MessageEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Response;

/**
 * A single feed. Can be subscribed to, unsubscribed from, or used to fetch a number of items in it.
 * It can also append items. What more could you wish fore?
 * */
public class Feed {

    static Gson gson = new Gson();
    final App app;
    final String name;
    final RetryStrategy retryStrategy;
    final BaseClient baseClient;

    String mostRecentReceivedItemId = null;
    String oldestReceivedItemID = null;
    boolean hasNext = true;

    private Feed(App app, String name, RetryStrategy retryStratregy) {
        this.name = name;
        this.app = app;
        this.retryStrategy  = retryStratregy;
        this.baseClient = app.client;
    }

    private ResumableSubscription subscription;

    /**
     * The simplest-possible way to subscribe to a feed. This fetches the last items in the feed, oldest to newest and then any new items as they get appended to the feed.
     * @param onItemListener The callback that triggers for each new item in a feed.
     * @param onErrorListener Error callback
     * */
    public void subscribe(OnItemListener onItemListener, ErrorListener onErrorListener){
        subscribe(null, onItemListener, onErrorListener, null);
    }

    /**
     * Subscribe to a feed, from the last item specified with ID
     * @param onItemListener The callback that triggers for each new item in a feed.
     * @param errorListener Error callback
     * @param onOpenListener Callback that triggers when the subscription is opened
     * @param lastItemId the ID of the last item we are subscribing from
     * */
    public void subscribe(final OnOpenListener onOpenListener, final OnItemListener onItemListener, final ErrorListener errorListener, final String lastItemId){

        if(lastItemId == null && mostRecentReceivedItemId == null) {
            fetchOlderItems(new OnItemsListener() {
                @Override
                public void onItems(List<Item> items) {
                    int numberOfItems = null != items ? items.size() : 0;
                    String mostRecentItemId = null;

                    if (numberOfItems > 0) {
                        mostRecentItemId = items.get(0).getId();
                        for (int i = numberOfItems - 1; i >= 0; i--) {
                            onItemListener.onItem(items.get(i));
                        }
                    }
                    subscription = baseClient.subscribe(url(), onOpenListener, new EventToItemTransformer(onItemListener), errorListener, lastItemId, retryStrategy);
                }
            }, errorListener);
        }
        else {
                subscription = baseClient.subscribe(url(), onOpenListener, new EventToItemTransformer(onItemListener), errorListener, lastItemId, retryStrategy);
        }
    }

    /**
     * Unsubscribe from the current subscription
     * @throws IllegalStateException if it's not subscribed.
     * */
    public void unsubscribe(){
        if(null != subscription && subscription.isSubscribed()) subscription.close();
        else throw new IllegalStateException("Subscription doesn't exist or is not subscribed!");
    }

    /**
     * Appends the list of items to the current feed.
     * @param items the list of items to append.
     *
     * */
    public void append(List<Item> items, final ErrorListener errorListener){
        //TODO: this should have an error callback
        Headers headers = new Headers.Builder()
                .add("Content-type", "application/json")
                .build();

        AppendRequestBody requestBody = new AppendRequestBody(items);

        //TODO: this is a DISGRACE too
        baseClient.request("POST", url(), headers, gson.toJson(requestBody), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                errorListener.onError(Error.fromThrowable(e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("FEED", "Append status "+ response.code());
            }
        });
    }

    /**
     * Appends the item to the current feed.
     * @param item the item to append.
     * */
    public void append(Item item, ErrorListener errorListener){
        List<Item> items = new ArrayList<>();
        items.add(item);
        append(items, errorListener);
    }

    /**
     * Utility method to fetch the last _n_ items from the last currently known item in the feed.
     * @param listener the callback that triggers on each retrieved item
     * */
    public void fetchOlderItems(final OnItemsListener listener, final ErrorListener errorListener){
        //TODO: this should have an error callback
        fetchItems(oldestReceivedItemID, 0, listener, errorListener);
    }

    /**
     * Utility method to fetch the previous _limit_ items from the last currently known item in the feed
     * @param limit number of items to fetch. If a feed has less than that items it will fetch as many as it can
     * @param listener the callback that triggers on each retrieved item */
    public void fetchOlderItems(int limit, final OnItemsListener listener, final ErrorListener errorListener){
        //TODO: this should have an error callback
        fetchItems(oldestReceivedItemID, limit, listener, errorListener);
    }

    /**
     * Method for fetching the previous _limit_ items, starting with the item with _lastItemId_
     * @param oldestItemId the oldest retrieved item in this feed
     * @param limit number of items to fetch. If a feed has less than that items it will fetch as many as it can. If this value is negative or zero it will use server-default value.
     * @param listener the callback that triggers on each retrieved item  */
    public void fetchItems(final String oldestItemId, int limit, final OnItemsListener listener, final ErrorListener errorListener){
        //TODO: this should have an error callback
        Headers headers = new Headers.Builder()
                .add("Content-Type", "application/json")
                .build();

        HttpUrl.Builder urlBuilder = HttpUrl.parse(url()).newBuilder();
        if(null != oldestItemId) urlBuilder.addQueryParameter("from_id", oldestItemId);
        if(limit > 0) urlBuilder.addQueryParameter("limit", String.valueOf(limit));

        if(hasNext){
            baseClient.request("GET", urlBuilder.toString(), headers, new Callback(){
                @Override
                public void onFailure(Call call, IOException e) {
                    app.logger.log(e, "onFailure getting items!");
                    errorListener.onError(Error.fromThrowable(e));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    app.logger.log("OnResponse");
                    FetchResponse fetchResponse = gson.fromJson(response.body().string(), FetchResponse.class);

                    if(fetchResponse.getNextId() != null){
                        hasNext = true;
                        Feed.this.oldestReceivedItemID = fetchResponse.getNextId();
                    }
                    else{
                        hasNext = false;
                    }
                    listener.onItems(fetchResponse.getItems());
                }
            });
        }
        else{
            listener.onItems(Collections.<Item>emptyList());
        }
    }

    private String url(){
        return String.format("%s/feeds/%s", app.baseUrl(), name);
    }

    private class EventToItemTransformer implements OnEventListener {

        private final OnItemListener onItemListener;

        EventToItemTransformer(OnItemListener onItemListener){
            this.onItemListener = onItemListener;
        }

        @Override
        public void onEvent(Event event) {
            MessageEvent message = (MessageEvent) event;
            onItemListener.onItem(new Item(message.getId(), message.getBody()));
        }
    }


    public static class Builder {
        private App app;
        private String name;
        private RetryStrategy retryStrategy;

        public Feed build(){
            if(retryStrategy == null) retryStrategy = new DefaultRetryStrategy();
            if(app == null) throw new IllegalStateException("App must not be null");
            if(name == null) throw new IllegalStateException("Feed name must not be null");
            return new Feed(app, name, retryStrategy);
        }

        /**
         * Sets the Pusher Platform {@link App} to use. This is mandatory.
         * */
        public Builder app(App app){
            this.app = app;
            return this;
        }

        /**
         * Sets the name of the current Feed. This is mandatory.
         * */
        public Builder name(String name){
            this.name = name;
            return this;
        }

        /**
         * The {@link RetryStrategy} that ensures reconnections happen when subscribing to this feed. If no strategy provided the {@link DefaultRetryStrategy} is used.
         * You can disable retrying by passing it an instance of {@link NoRetryStrategy}.
         * */
        public Builder retryStrategy(RetryStrategy retryStrategy){
            this.retryStrategy = retryStrategy;
            return this;
        }
    }
}
