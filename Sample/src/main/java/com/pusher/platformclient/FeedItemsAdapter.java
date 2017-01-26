package com.pusher.platformclient;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pusher.platform.feeds.Item;

import java.util.ArrayList;
import java.util.List;

public class FeedItemsAdapter extends RecyclerView.Adapter<FeedViewHolder> {

    private ArrayList<Item> feedItems = new ArrayList<>();

    public void addItemToTop(Item item){
        feedItems.add(0, item);
        notifyItemInserted(0);
    }

    public void addItemsToBottom(List<Item> items){
        int firstIndex = feedItems.size();
        feedItems.addAll(items);
        notifyItemRangeInserted(firstIndex, items.size());
    }

    @Override
    public FeedViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.feed_item, parent, false);
        return new FeedViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(FeedViewHolder holder, int position) {
        holder.setItem(feedItems.get(position));
    }

    @Override
    public int getItemCount() {
        return feedItems.size();
    }
}
