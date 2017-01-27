package com.pusher.platform.auth;

import com.google.gson.annotations.SerializedName;

class AuthResponse {
    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("expires_in")
    private long expiresIn;

    @SerializedName("refresh_token")
    private String refreshToken;

    @SerializedName("token_type")
    private String tokenType; //TODO: not sure we need this, it's always Bearer

    String getAccessToken() {
        return accessToken;
    }

    long getExpiresIn() {
        return expiresIn;
    }

    String getRefreshToken() {
        return refreshToken;
    }
}
