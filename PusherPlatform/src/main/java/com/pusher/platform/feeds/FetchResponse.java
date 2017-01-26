package com.pusher.platform.feeds;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class FetchResponse {
    private List<Item> items;
    @SerializedName("next_id") private String nextId;

    public List<Item> getItems() {
        return items;
    }

    public String getNextId() {
        return nextId;
    }
}
