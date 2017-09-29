package com.pusher.platform.sample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.pusher.platform.*
import com.pusher.platform.logger.AndroidLogger
import com.pusher.platform.logger.LogLevel
import com.pusher.platform.tokenProvider.TokenProvider
import elements.Error
import elements.Subscription
//import okhttp3.Response
import kotlinx.android.synthetic.main.activity_sample.*
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import retrofit2.Retrofit

class SampleActivity : AppCompatActivity() {

    var subscription: Subscription? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample)
        val pusherPlatform = Instance(instanceId = "v1:us1:bb53a31e-bab3-4dfa-a52b-adaa44f14119", serviceName = "feeds", serviceVersion = "v1", logger = AndroidLogger(threshold = LogLevel.VERBOSE), context = this)
        val listeners = SubscriptionListeners(
                onOpen = { headers -> Log.d("PP", "OnOpen ${headers}") },
                onSubscribe = { Log.d("PP", "onSubscribe") },
                onRetrying = { Log.d("PP", "onRetrying") },
                onEvent = { event -> Log.d("PP", "onEvent ${event}") },
                onEnd = { eosEvent -> Log.d("PP", "onEnd ${eosEvent}")},
                onError = { error -> Log.d("PP", "onError ${error}")}
        )


        this.get_request_btn.setOnClickListener {

            val requestInProgress = pusherPlatform.request(
                options = RequestOptions(path = "feeds/my-feed/items" ),
                onSuccess = { response -> Log.d("PP", response.body()!!.string()) },
                onFailure = { error -> Log.d("PP", error.toString())}

            )
        }

        val client = OkHttpClient()

        val tokenProvider: TokenProvider = object: TokenProvider {

            override fun fetchToken(tokenParams: Any?, onSuccess: (String) -> Unit, onFailure: (Error) -> Unit): Cancelable {

                val requestBody = RequestBody.create(
                        MediaType.parse("application/x-www-form-urlencoded"),
                )
                val request = Request.Builder()
                        .url("http://localhost:3000/feeds/tokens")
                        .post(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), ))

            }


            override fun clearToken(token: String?) {
                //We ain't gonna cache anything because caching is for losers
            }
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

        }

        this.unsubscribe.setOnClickListener {
            subscription?.unsubscribe()
        }

    }
}

