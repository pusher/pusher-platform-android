package com.pusher.platform.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.ConnectivityManager


class AndroidConnectivityHelper(val context: Context) : ConnectivityHelper {

    var action: (() -> Unit )? = null

    val receiver =  object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            if(isConnected()){
                this@AndroidConnectivityHelper.context.unregisterReceiver(this)
                action?.invoke()
            }
        }
    }

    override fun isConnected(): Boolean {
        val connectivityManager= context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo

        return activeNetwork != null && activeNetwork!!.isConnectedOrConnecting
    }

    override fun onConnected(retryNow: () -> Unit) {
        if(isConnected()){
            retryNow()
        } else{
            action = retryNow
            context.registerReceiver(receiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        }
    }

    override fun cancel(){
        context.unregisterReceiver(receiver)
    }
}