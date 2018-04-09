package com.pusher.platform

import android.content.Context
import com.pusher.SdkInfo
import com.pusher.platform.logger.AndroidLogger
import com.pusher.platform.logger.LogLevel
import com.pusher.platform.network.AndroidConnectivityHelper
import com.pusher.platform.network.ConnectivityHelper

/**
 * Provides default values for dependencies when using Android.
 */
data class AndroidDependencies(
    private val context: Context,
    override val sdkInfo: SdkInfo = SdkInfo(
        product = "pusher-platform",
        sdkVersion = "0.0.0",
        platform = "Android",
        language = "Kotlin"
    )
) : PlatformDependencies {

    override val logger by lazy { AndroidLogger(threshold = LogLevel.DEBUG) }
    override val scheduler: Scheduler by lazy { BackgroundScheduler() }
    override val mainScheduler: MainThreadScheduler by lazy { ForegroundScheduler() }
    override val mediaTypeResolver: MediaTypeResolver by lazy { AndroidMediaTypeResolver() }
    override val connectivityHelper: ConnectivityHelper by lazy { AndroidConnectivityHelper(context) }


}
