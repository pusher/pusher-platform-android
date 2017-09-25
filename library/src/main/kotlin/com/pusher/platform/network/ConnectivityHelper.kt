package com.pusher.platform.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.ConnectivityManager
import android.util.Log
import com.pusher.platform.retrying.RetryStrategyOptions


class ConnectivityHelper(val context: Context, retryStrategyOptions: RetryStrategyOptions) {

    var action: (() -> Unit )? = null

    val receiver =  object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            if(isConnected()){
                this@ConnectivityHelper.context.unregisterReceiver(this)
                action?.invoke()
            }
        }
    }


    fun isConnected(): Boolean {
        val connectivityManager= context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo

        return activeNetwork != null && activeNetwork!!.isConnectedOrConnecting
    }

    fun onConnected(retryNow: () -> Unit) {
        if(isConnected()){
            retryNow()
        } else{
            action = retryNow
            context.registerReceiver(receiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        }
    }

    fun cancel(){
        context.unregisterReceiver(receiver)
    }
}