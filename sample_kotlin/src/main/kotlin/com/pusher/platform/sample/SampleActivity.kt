package com.pusher.platform.sample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.pusher.platform.Instance
import com.pusher.platform.R
import com.pusher.platform.SubscriptionListeners
import com.pusher.platform.logger.AndroidLogger
import com.pusher.platform.logger.LogLevel
import com.pusher.platform.network.ConnectivityHelper
import elements.EOSEvent

class SampleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample)
        val pusherPlatform = Instance(instanceId = "v1:us1:bb53a31e-bab3-4dfa-a52b-adaa44f14119", serviceName = "feeds", serviceVersion = "v1", logger = AndroidLogger(threshold = LogLevel.VERBOSE), context = this)
        val listeners = SubscriptionListeners(
                onOpen = { headers -> Log.d("PP", headers.toString()) },
                onSubscribe = { Log.d("PP", "onSubscribe") },
                onRetrying = { Log.d("PP", "onRetrying") },
                onEvent = { event -> Log.d("PP", event.toString()) },
                onEnd = { eosEvent -> Log.d("PP", eosEvent.toString())},
                onError = { error -> Log.d("PP", error.toString())}
        )
        //TODO Remove this when happy with the actual impl
//        pusherPlatform.justFuckingSubscribe(path = "feeds/my-feed/items", listeners = listeners)
//
//        val connectivityHelper = ConnectivityHelper(this)
//        connectivityHelper.onConnected { Log.d("FOOO", "ACTION") }


        pusherPlatform.subscribeResuming(path = "feeds/my-feed/items", listeners = listeners)
    }
}

