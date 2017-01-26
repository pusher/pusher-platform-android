package com.pusher.platform.feeds;

import android.util.Log;

import com.google.gson.Gson;
import com.pusher.platform.App;
import com.pusher.platform.BaseClient;
import com.pusher.platform.retrying.DefaultRetryStrategy;
import com.pusher.platform.retrying.RetryStrategy;
import com.pusher.platform.subscription.OnErrorListener;
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
 * */
public class Feed {

    private static Gson gson = new Gson();

    private final App app;
    private final String name;
    private final RetryStrategy retryStrategy;
    private BaseClient baseClient;
    private String lastReceivedItemId = null;
    private String oldestReceivedItemID = null;
    private boolean hasNext = true;


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
    public void subscribe(OnItemListener onItemListener, OnErrorListener onErrorListener){
        subscribe(null, onItemListener, onErrorListener, null);
    }

    /**
     * Subscribe to a feed, from the last item specified with ID
     * @param onItemListener The callback that triggers for each new item in a feed.
     * @param onErrorListener Error callback
     * @param onOpenListener Callback that triggers when the subscription is opened
     * @param lastItemId the ID of the last item we are subscribing from
     * */
    public void subscribe(final OnOpenListener onOpenListener, final OnItemListener onItemListener, final OnErrorListener onErrorListener, final String lastItemId){

        if(lastItemId == null && lastReceivedItemId == null){
            fetchOlderItems(new OnItemsListener() {
                @Override
                public void onItems(List<Item> items) {
                    int numberOfItems = null != items ? items.size() : 0;
                    String mostRecentItemId = null;

                    if(numberOfItems > 0){
                        mostRecentItemId = items.get(0).getId();
                        for(int i = numberOfItems-1; i >= 0; i--){
                            onItemListener.onItem(items.get(i));
                        }
                    }
                    subscription = baseClient.subscribe(url(), onOpenListener, new EventToItemTransformer(onItemListener), onErrorListener, lastItemId, retryStrategy);
                }
            });
        }

        else{
            subscription = baseClient.subscribe(url(), onOpenListener, new EventToItemTransformer(onItemListener), onErrorListener, lastItemId, retryStrategy);
        }
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

    /**
     * Unsubscribe from the current subscription
     * @throws IllegalStateException if it's not subscribed.
     * */
    public void unsubscribe(){
        if(null != subscription && subscription.isSubscribed()) subscription.close();
        else throw new IllegalStateException("Subscription doesn't exist or is not subscribed!");
    }

    /**
     * Appends the item to the current feed.
     *
     * */
    public void append(List<Item> items){

        Headers headers = new Headers.Builder()
                .add("Content-type", "application/json")
                .build();

        AppendRequestBody requestBody = new AppendRequestBody(items);

        //TODO: this is a DISGRACE too
        baseClient.request("POST", url(), headers, gson.toJson(requestBody), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("FEED", "Append status "+ response.code());
            }
        });
    }

    public void append(Item item){

        List<Item> items = new ArrayList<>();
        items.add(item);
        append(items);
    }

    /**
     * Utility method to fetch the last _n_ items from the last currently known item in the feed.
     * @param listener the callback that triggers on each retrieved item
     * */
    public void fetchOlderItems(final OnItemsListener listener){
        fetchItems(oldestReceivedItemID, 0, listener);
    }

    /**
     * Utility method to fetch the previous _limit_ items from the last currently known item in the feed
     * @param limit number of items to fetch. If a feed has less than that items it will fetch as many as it can
     * @param listener the callback that triggers on each retrieved item */
    public void fetchOlderItems(int limit, final OnItemsListener listener){
        fetchItems(oldestReceivedItemID, limit, listener);
    }

    /**
     * Method for fetching the previous _limit_ items, starting with the item with _lastItemId_
     * @param oldestItemId the oldest retrieved item in this feed
     * @param limit number of items to fetch. If a feed has less than that items it will fetch as many as it can. If this value is negative or zero it will use server-default value.
     * @param listener the callback that triggers on each retrieved item  */
    public void fetchItems(final String oldestItemId, int limit, final OnItemsListener listener){
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

    public static class Builder {
        private App app;
        private String name;
        private RetryStrategy retryStrategy;

        public Feed build(){
            if(retryStrategy == null) retryStrategy = new DefaultRetryStrategy();
            return new Feed(app, name, retryStrategy);
        }

        public Builder app(App app){
            this.app = app;
            return this;
        }

        public Builder name(String name){
            this.name = name;
            return this;
        }

        /**
         * The {@link RetryStrategy} that ensures reconnections happen when subscribing to this feed. If no strategy provided the {@link DefaultRetryStrategy} is used.
         * */
        public Builder retryStrategy(RetryStrategy retryStrategy){
            this.retryStrategy = retryStrategy;
            return this;
        }
    }
}
