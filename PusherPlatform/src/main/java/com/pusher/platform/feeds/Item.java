package com.pusher.platform.feeds;

import com.google.gson.JsonElement;

public class Item {

//    Gson gson = new Gson();

    private String id;
    //TODO: allow retrofit-like automagic serialization when a feed is created?
    private JsonElement data;

    public Item(String id, JsonElement data){
        this.id = id;
        this.data = data;
    }

//    public Item(String data){
//        this.data = gson.toJsonTree(data);
//    }

    public String getId() {
        return id;
    }

    public String getData() {
        return data.toString();
    }
}
