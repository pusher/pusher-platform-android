package com.pusher.platformclient;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.pusher.platform.feeds.Item;

public class FeedViewHolder extends RecyclerView.ViewHolder {

    TextView id;
    TextView data;

    public void setItem(Item item){
        id.setText(item.getId());
        data.setText(item.getData());
    }

    public FeedViewHolder(View itemView) {
        super(itemView);
        id = (TextView) itemView.findViewById(R.id.text_item_id);
        data = (TextView) itemView.findViewById(R.id.text_item_data);
    }
}
