package com.pusher.platform.subscription.event;

import com.google.gson.JsonElement;

import java.util.Map;

public class MessageEvent implements Event {

    private String id;
    private Map<String, String> headers;
    private JsonElement body;

    public MessageEvent(String id, Map<String, String> headers, JsonElement body){
        this.id = id;
        this.headers = headers;
        this.body = body;
    }

    public String getId() {
        return id;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public JsonElement getBody() {
        return body;
    }
}
