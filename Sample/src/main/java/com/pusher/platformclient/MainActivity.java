package com.pusher.platformclient;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.pusher.platform.App;
import com.pusher.platform.auth.AnonymousAuthorizer;
import com.pusher.platform.auth.Authorizer;
import com.pusher.platform.auth.SharedPreferencesAuthorizer;
import com.pusher.platform.feeds.Feed;
import com.pusher.platform.feeds.Item;
import com.pusher.platform.feeds.OnItemListener;
import com.pusher.platform.feeds.OnItemsListener;
import com.pusher.platform.logger.SystemLogger;
import com.pusher.platform.subscription.OnErrorListener;
import com.pusher.platform.subscription.OnOpenListener;
import com.pusher.platform.subscription.ResumableSubscription;
import com.pusher.platform.subscription.SubscriptionException;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FeedItemsAdapter adapter;
    private Button subscribe;
    private Button fetch;
    private TextView status;
    private Feed feed;
    private App app;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        subscribe = (Button) findViewById(R.id.btn_subscribe);
        fetch = (Button) findViewById(R.id.btn_fetchNext);
        status = (TextView) findViewById(R.id.text_status);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerview);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FeedItemsAdapter();
        recyclerView.setAdapter(adapter);



        Authorizer authorizer = new SharedPreferencesAuthorizer.Builder()
                .endpoint("YOUR_AUTHORIZER_ENDPOINT_HERE")
                .context(getApplicationContext())
                .build();

        Authorizer anonAuthorizer = new AnonymousAuthorizer(null);

        app = new App.Builder()
                .id("YOUR_APP_ID")
                .authorizer(anonAuthorizer)
                .logger(new SystemLogger())
                .build();

        feed = new Feed.Builder()
                .app(app)
                .name("FEED_NAME")
                .build();

        app.setUserId("YOUR_APP_USERS_NAME");

        subscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (subscribe.getText().toString().equalsIgnoreCase("subscribe")) {
                    performSubscribe();
                } else {
                    performUnsubscribe();

                }
            }
        });

        fetch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performFetch();
            }
        });
    }

    private void performFetch() {
        feed.fetchOlderItems(new OnItemsListener() {
            @Override
            public void onItems(final List<Item> items) {
                if (items == null || items.size() <= 0) {
                    updateStatus("No more items in the feed!");
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.addItemsToBottom(items);
                        }
                    });
                    updateStatus("Added " + items.size() + " items!");
                }
            }
        });
    }

    private void performUnsubscribe() {
        feed.unsubscribe();
        updateStatus("Not subscribed");
        subscribe.setText("Subscribe");
    }

    private void performSubscribe() {
        feed.subscribe(new OnOpenListener() {
            @Override
            public void onOpen() {
                updateStatus("Subscription opened!");
            }
        }, new OnItemListener() {
            @Override
            public void onItem(final Item item) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.addItemToTop(item);
                        recyclerView.scrollToPosition(0);
                    }
                });
            }
        }, new OnErrorListener() {
            @Override
            public void onError(SubscriptionException exception) {
                updateStatus("onError! " + exception.type + " " + exception.getMessage());
                exception.printStackTrace();
            }
        }, null);

        subscribe.setText("Unsubscribe");
    }

    private void updateStatus(final String newStatus) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                status.setText(newStatus);
            }
        });

        Log.d("STATUS", newStatus);
    }
}


