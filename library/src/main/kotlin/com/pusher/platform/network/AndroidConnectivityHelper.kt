package com.pusher.platform.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import com.pusher.platform.android.content.connectivityManager
import com.pusher.platform.android.content.onReceiveBroadcast
import java.util.concurrent.LinkedBlockingQueue

class AndroidConnectivityHelper(private val context: Context) : ConnectivityHelper {

    private val retryActions = LinkedBlockingQueue<() -> Unit>()
    private val receiver: BroadcastReceiver = onReceiveBroadcast { self, _, _ ->
        if (isConnected()) {
            this@AndroidConnectivityHelper.context.unregisterReceiver(self)
            while (retryActions.isNotEmpty()) {
                retryActions.take().invoke()
            }
        }
    }

    override fun isConnected(): Boolean =
        context.connectivityManager.activeNetworkInfo?.isConnectedOrConnecting ?: false

    override fun onConnected(retryNow: () -> Unit) {
        when {
            isConnected() -> retryNow()
            else -> {
                retryActions += retryNow
                context.registerReceiver(receiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
            }
        }
    }

    override fun cancel() =
        context.unregisterReceiver(receiver)

}