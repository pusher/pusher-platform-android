package com.pusher.platform.sample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.pusher.platform.Instance
import com.pusher.platform.R
import com.pusher.platform.SubscriptionListeners
import elements.EOSEvent

class SampleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample)
        val pusherPlatform = Instance(instanceId = "v1:us1:bb53a31e-bab3-4dfa-a52b-adaa44f14119", serviceName = "feeds", serviceVersion = "v1")
        val listeners = SubscriptionListeners(
                onOpen = { headers -> Log.d("PP", headers.toString()) },
                onSubscribe = { Log.d("PP", "onSubscribe") },
                onRetrying = { Log.d("PP", "onRetrying") },
                onEvent = { event -> Log.d("PP", event.toString()) },
                onEnd = { eosEvent -> Log.d("PP", eosEvent.toString())},
                onError = { error -> Log.d("PP", error.toString())}
        )
        pusherPlatform.justFuckingSubscribe(path = "feeds/my-feed/items", listeners = listeners, headers = null)
    }
}
