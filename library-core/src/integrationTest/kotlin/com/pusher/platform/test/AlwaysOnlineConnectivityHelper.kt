package com.pusher.platform.test

import com.pusher.platform.network.ConnectivityHelper

object AlwaysOnlineConnectivityHelper : ConnectivityHelper {
    override fun isConnected(): Boolean = true
    override fun onConnected(retryNow: () -> Unit) = retryNow()
    override fun cancel() = Unit
}
