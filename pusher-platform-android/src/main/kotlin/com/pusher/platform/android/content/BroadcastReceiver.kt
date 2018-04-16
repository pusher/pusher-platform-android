package com.pusher.platform.android.content

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

fun onReceiveBroadcast(f: (BroadcastReceiver, Context, Intent) -> Unit): BroadcastReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) =
        f(this, context, intent)
}