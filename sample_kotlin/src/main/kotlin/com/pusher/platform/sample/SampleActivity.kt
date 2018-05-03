package com.pusher.platform.sample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.pusher.platform.*
import com.pusher.platform.network.DataParser
import com.pusher.platform.network.Futures
import com.pusher.platform.network.wait
import com.pusher.platform.tokenProvider.TokenProvider
import com.pusher.util.Result
import com.pusher.util.asFailure
import com.pusher.util.asSuccess
import elements.Error
import elements.Errors
import elements.Subscription
import kotlinx.android.synthetic.main.activity_sample.*
import kotlinx.coroutines.experimental.launch
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.Future

private const val INSTANCE_LOCATOR = "YOUR_INSTANCE_LOCATOR"

class SampleActivity : AppCompatActivity() {

    private var subscription: Subscription? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample)

        val pusherPlatform = Instance(
            locator = INSTANCE_LOCATOR,
            serviceName = "feeds",
            serviceVersion = "v1",
            dependencies = AndroidDependencies()
        )

        val listeners = SubscriptionListeners<JsonElement>(
            onOpen = { headers -> Log.d("PP", "OnOpen $headers") },
            onSubscribe = { Log.d("PP", "onSubscribe") },
            onRetrying = { Log.d("PP", "onRetrying") },
            onEvent = { event -> Log.d("PP", "onEvent $event") },
            onEnd = { eosEvent -> Log.d("PP", "onEnd $eosEvent") },
            onError = { error -> Log.d("PP", "onError $error") }
        )

        this.get_request_btn.setOnClickListener {
            launch {
                val result = pusherPlatform.request(
                    options = RequestOptions(path = "feeds/my-feed/items"),
                    responseParser = JSON_ELEMENT_BODY_PARSER
                ).wait()
                Log.d("PP", "result $result")
            }
        }

        this.get_request_authorized_btn.setOnClickListener {
            TODO()
        }

        this.subscribe_non_resuming_btn.setOnClickListener {
            subscription = pusherPlatform.subscribeNonResuming(
                path = "feeds/my-feed/items",
                listeners = listeners,
                messageParser = JSON_ELEMENT_BODY_PARSER
            )
        }

        this.subscribe_resuming_btn.setOnClickListener {
            subscription = pusherPlatform.subscribeResuming(
                path = "feeds/my-feed/items",
                listeners = listeners,
                messageParser = JSON_ELEMENT_BODY_PARSER
            )
        }

        this.subscribe_authorized_btn.setOnClickListener {
            subscription = pusherPlatform.subscribeNonResuming(
                path = "firehose/items",
                listeners = listeners,
                messageParser = JSON_ELEMENT_BODY_PARSER,
                tokenProvider = MyTokenProvider(client, gson),
                tokenParams = SampleTokenParams(path = "firehose/items", authorizePath = "path/tokens")
            )
        }

        this.unsubscribe.setOnClickListener {
            subscription?.unsubscribe()
        }
    }

    private val client = OkHttpClient()


    data class SampleTokenParams(val path: String, val action: String = "READ", val authorizePath: String)

    data class FeedsTokenResponse(

        val accessToken: String,
        val tokenType: String,
        val expiresIn: String,
        val refreshToken: String
    )

    class MyTokenProvider(
        private val client: OkHttpClient,
        private val gson: Gson
    ) : TokenProvider {

        override fun fetchToken(tokenParams: Any?): Future<Result<String, Error>> {
            if (tokenParams is SampleTokenParams) {

                val requestBody = FormBody.Builder()
                    .add("path", tokenParams.path)
                    .add("action", tokenParams.action)
                    .add("grant_type", "client_credentials")
                    .build()

                val request = Request.Builder()
                    .url("http://10.0.2.2:3000/${tokenParams.authorizePath}")
                    .post(requestBody)
                    .build()


                val call = client.newCall(request)

                return Futures.schedule {

                    call.execute().let { response ->
                        when {
                            response != null && response.code() == 200 ->
                                gson.fromJson(response.body()!!.charStream(), FeedsTokenResponse::class.java).accessToken.asSuccess()
                            else -> elements.ErrorResponse(
                                statusCode = response!!.code(),
                                headers = response.headers().toMultimap(),
                                error = response.body().toString()
                            ).asFailure<String, Error>()
                        }
                    }

                }
            } else {
                throw kotlin.Error("Wrong token params!")
            }
        }

        override fun clearToken(token: String?) {
            //We ain't gonna cache anything because caching is for losers
        }

    }
}

private val gson = GsonBuilder()
    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
    .create()

private val JSON_ELEMENT_BODY_PARSER : DataParser<JsonElement> = {
    try {
        gson.fromJson(it, JsonElement::class.java).asSuccess()
    } catch (e: Throwable) {
        Errors.other(e).asFailure()
    }
}
