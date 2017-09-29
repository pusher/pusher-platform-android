package com.pusher.platform.sample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.pusher.platform.Instance
import com.pusher.platform.R
import com.pusher.platform.RequestOptions
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.AndroidLogger
import com.pusher.platform.logger.LogLevel
import elements.Subscription
//import okhttp3.Response
import kotlinx.android.synthetic.main.activity_sample.*

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

