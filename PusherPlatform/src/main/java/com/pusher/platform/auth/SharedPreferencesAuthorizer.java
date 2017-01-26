package com.pusher.platform.auth;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.pusher.platform.util.TimeUtil;

import java.io.IOException;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

public class SharedPreferencesAuthorizer implements Authorizer {

    private static final String ACCESS_TOKEN = "PPAuthAccessToken";
    private static final String REFRESH_TOKEN = "PPAuthRefreshToken";
    private static final String ACCESS_TOKEN_EXPIRY = "PPAuthAccessTokenExpiry";
    private static final String USER_ID = "PPAuthUserId";
    private static final String PREFERENCES = "PPAuthPreferences";

    private final String endpoint;
    private OkHttpClient httpClient; //TODO find a way for this sucker to be final
    private final TimeUtil timeUtil;
    private final Context applicationContext;
    private static Gson gson = new Gson();
    private SharedPreferences sharedPreferences;

    private int numberOfTries = 0;
    private final static int MAX_NUMBER_OF_TRIES = 3;

    private SharedPreferencesAuthorizer(String endpoint, OkHttpClient httpClient, TimeUtil timeUtil, Context applicationContext) {
        this.endpoint = endpoint;
        this.httpClient = httpClient;
        this.timeUtil = timeUtil;
        this.applicationContext = applicationContext;

        sharedPreferences = applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    @Override
    public void performRequest(final Request request, final Callback callback) {

        //This shouldn't JUST take a callback - not the right way to terminate requests otherwise.
        if(numberOfTries >= MAX_NUMBER_OF_TRIES){
            //TODO: terminate
        }

        //Check if we have the token
        if(accessToken().length() > 0) {
            //Check if expiration is valid
            if(expiry().compareTo(timeUtil.now()) > 0) {
                Request authenticatedRequest = request.newBuilder()
                        .addHeader("Authorization", "Bearer " + accessToken())
                        .build();

                numberOfTries++;
                httpClient.newCall(authenticatedRequest).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        numberOfTries = 0;
                        callback.onFailure(call, e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if(response.code() == HTTP_UNAUTHORIZED){

                            requestTokenPairWithRefreshToken(request, callback);
                        }
                        else {
                            numberOfTries = 0;
                            callback.onResponse(call, response);
                        }
                    }
                });
            }
            //Token we have has expired. Try to refresh
            else {
                requestTokenPairWithRefreshToken(request, callback);
            }
        }
        //No token, we assume we can somehow get credentials. Terminate otherwise? //TODO: decide on this
        else {
            requestTokenPairWithCredentials(request, callback);
        }
    }

    @Override
    public void setHttpClient(OkHttpClient okHttpClient) {
        this.httpClient = okHttpClient;
    }

    @Override
    public void setUserId(String userId) {
        sharedPreferences.edit().putString(USER_ID, userId).apply();
    }

    private void requestTokenPair(final RequestBody requestbody, final Request request, final Callback callback){
        Request authRequest = new Request.Builder()
                .url(endpoint)
                .post(requestbody)
                .build();

        httpClient.newCall(authRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(call, e);
                numberOfTries = 0;
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == HTTP_OK) {
                    String body = response.body().string();
                    AuthResponse authResponse = gson.fromJson(body, AuthResponse.class);

                    storeTokens(authResponse);
                    performRequest(request, callback);
                }
            }
        });
    }

    private void requestTokenPairWithRefreshToken(final Request request, final Callback callback) {
        RequestBody body = new FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken())
                .build();

        requestTokenPair(body, request, callback);
    }

   private void requestTokenPairWithCredentials(final Request request, final Callback callback){
        RequestBody body = new FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("user_id", userId()) //TODO: this is gonna be fun.
                .build();
        requestTokenPair(body, request, callback);
    }

    //TODO: this SHOULD be more secure. (Although if some1 steals your device then access to feeds refresh tokens is likely not your biggest concern.
    private void storeTokens(AuthResponse authResponse) {
        sharedPreferences.edit()
                .putString(ACCESS_TOKEN, authResponse.getAccessToken())
                .putString(REFRESH_TOKEN, authResponse.getRefreshToken())
                .putString(ACCESS_TOKEN_EXPIRY, timeUtil.dateFromSecondsInTheFuture(authResponse.getExpiresIn())).apply();
    }

    private String accessToken(){
        return sharedPreferences.getString(ACCESS_TOKEN, "");
    }

    private String refreshToken(){
        return sharedPreferences.getString(REFRESH_TOKEN, "");
    }

    private Date expiry(){
        return timeUtil.dateFromString(sharedPreferences.getString(ACCESS_TOKEN_EXPIRY, ""));
    }

    private String userId(){
        return sharedPreferences.getString(USER_ID, "");
    }

    public static class Builder {

        private String endpoint;
        private OkHttpClient httpClient;
        private TimeUtil timeUtil;
        private Context context;

        public Builder endpoint(String endpoint){
            this.endpoint = endpoint;
            return this;
        }

        public Builder httpClient(OkHttpClient client){
            this.httpClient = client;
            return this;
        }

        public Builder timeUtil(TimeUtil timeUtil){
            this.timeUtil = timeUtil;
            return this;
        }

        public Builder context(Context context){
            this.context = context;
            return this;
        }

        public SharedPreferencesAuthorizer build(){
            if(null == endpoint || endpoint.length() <= 0) throw new IllegalStateException("Endpoint must be specified");
//            if(null == httpClient) throw new IllegalStateException("OKHttpClientmust not be null");
            if(null == context) throw new IllegalStateException("Context must not be null");
            if(null == timeUtil){
                timeUtil = new TimeUtil(); //this is just so we can mock time in tests
            }
            return new SharedPreferencesAuthorizer(endpoint, httpClient, timeUtil, context);
        }
    }
}
