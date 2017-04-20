package com.pusher.platform.feeds;

import com.google.gson.annotations.SerializedName;

public class AppendResponse {

    @SerializedName("item_id") private String itemId;

    public String getItemId() {
        return itemId;
    }
}
