package com.pusher.platform.network

interface ConnectivityHelper {
    fun isConnected(): Boolean
    fun onConnected(retryNow: () -> Unit)
    fun cancel()
}