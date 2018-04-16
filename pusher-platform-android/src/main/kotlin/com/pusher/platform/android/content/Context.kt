package com.pusher.platform.android.content

import android.content.Context
import android.net.ConnectivityManager

val Context.connectivityManager: ConnectivityManager
    get() = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager