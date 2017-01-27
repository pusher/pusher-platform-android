package com.pusher.platform.feeds;

import com.google.gson.annotations.SerializedName;

import java.util.List;

class FetchResponse {
    private List<Item> items;
    @SerializedName("next_id") private String nextId;

    List<Item> getItems() {
        return items;
    }

    String getNextId() {
        return nextId;
    }
}
