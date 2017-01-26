package com.pusher.platform.feeds;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class Item {

    static Gson gson = new Gson();

    private String id;
    //TODO: allow retrofit-like automagic serialization when a feed is created?
    private JsonElement data;

    public Item(String id, JsonElement data){
        this.id = id;
        this.data = data;
    }

    //TODO: this is a DISGRACE
    public Item(String data){
        this.data = gson.toJsonTree(data);
    }

    public String getId() {
        return id;
    }

    public String getData() {
        return data.toString();
    }
}
