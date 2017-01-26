package com.pusher.platform.feeds;

import java.util.List;

public class AppendRequestBody {
    private List<Item> items;

    public AppendRequestBody(List<Item> items){
        this.items = items;
    }
}
