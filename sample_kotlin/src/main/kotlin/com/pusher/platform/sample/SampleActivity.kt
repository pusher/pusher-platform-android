package com.pusher.platform.sample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.pusher.platform.*
import com.pusher.platform.tokenProvider.TokenProvider
import elements.Error
import elements.NetworkError
import elements.Subscription
import kotlinx.android.synthetic.main.activity_sample.*
import okhttp3.*
import java.io.IOException

private const val INSTANCE_LOCATOR = "YOUR_INSTANCE_LOCATOR"

class SampleActivity: AppCompatActivity() {

    private var subscription: Subscription? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample)

        val pusherPlatform = AndroidInstance(
                locator = INSTANCE_LOCATOR,
                serviceName = "feeds",
                serviceVersion = "v1",
                context = this)

        val listeners = SubscriptionListeners(
                onOpen = { headers -> Log.d("PP", "OnOpen $headers") },
                onSubscribe = { Log.d("PP", "onSubscribe") },
                onRetrying = { Log.d("PP", "onRetrying") },
                onEvent = { event -> Log.d("PP", "onEvent $event") },
                onEnd = { eosEvent -> Log.d("PP", "onEnd $eosEvent")},
                onError = { error -> Log.d("PP", "onError $error")}
        )

        this.get_request_btn.setOnClickListener {

            pusherPlatform.request(
                options = RequestOptions(path = "feeds/my-feed/items" ),
                onSuccess = { response -> Log.d("PP", response.body()!!.string()) },
                onFailure = { error -> Log.d("PP", error.toString())}

            )
        }

        this.get_request_authorized_btn.setOnClickListener{

            TODO()
        }

        this.subscribe_non_resuming_btn.setOnClickListener{
            subscription = pusherPlatform.subscribeNonResuming(path = "feeds/my-feed/items", listeners = listeners)

        }

        this.subscribe_resuming_btn.setOnClickListener {
            subscription = pusherPlatform.subscribeResuming(path = "feeds/my-feed/items", listeners = listeners)

        }

        this.subscribe_authorized_btn.setOnClickListener{
            subscription = pusherPlatform.subscribeNonResuming(
                    path = "firehose/items",
                    listeners = listeners,
                    tokenProvider = MyTokenProvider(client, gson),
                    tokenParams = SampleTokenParams(path = "firehose/items", authorizePath = "path/tokens")
            )
        }

        this.unsubscribe.setOnClickListener {
            subscription?.unsubscribe()
        }
    }

    val client = OkHttpClient()

    val gson = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()

    data class SampleTokenParams(val path: String, val action: String = "READ", val authorizePath: String)

    data class FeedsTokenResponse(

            val accessToken: String,
            val tokenType: String,
            val expiresIn: String,
            val refreshToken: String
    )


    class MyTokenProvider(val client: OkHttpClient, val gson: Gson): TokenProvider {
        var call: Call? = null
        override fun fetchToken(tokenParams: Any?, onSuccess: (String) -> Unit, onFailure: (Error) -> Unit): Cancelable {


            if(tokenParams is SampleTokenParams){

                val requestBody = FormBody.Builder()
                        .add("path", tokenParams.path)
                        .add("action", tokenParams.action)
                        .add("grant_type", "client_credentials")
                        .build()

                val request = Request.Builder()
                        .url("http://10.0.2.2:3000/${tokenParams.authorizePath}")
                        .post(requestBody)
                        .build()


                call = client.newCall(request)
                call!!.enqueue( object: Callback {

                    override fun onResponse(call: Call?, response: Response?) {

                        if(response != null && response.code() == 200) {
                            val body = gson.fromJson<FeedsTokenResponse>(response.body()!!.charStream(), FeedsTokenResponse::class.java)
                            onSuccess(body.accessToken)
                        }

                        else{
                            onFailure(elements.ErrorResponse(
                                    statusCode = response!!.code(),
                                    headers = response.headers().toMultimap(),
                                    error = response.body().toString()
                            ))
                        }
                    }

                    override fun onFailure(call: Call?, e: IOException?) {
                        onFailure(NetworkError("Failed! $e"))
                    }

                })
            }

            else{
                throw kotlin.Error("Wrong token params!")
            }

            return object: Cancelable {
                override fun cancel() {
                    call?.cancel()
                }
            }
        }

        override fun clearToken(token: String?) {
            //We ain't gonna cache anything because caching is for losers
        }

    }
}
