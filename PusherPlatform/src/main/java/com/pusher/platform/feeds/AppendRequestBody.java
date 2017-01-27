package com.pusher.platform.feeds;

import java.util.List;

class AppendRequestBody {
    private List<Item> items;

    AppendRequestBody(List<Item> items){
        this.items = items;
    }
}
