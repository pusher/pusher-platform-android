package com.pusher.platform

import android.content.Context
import com.pusher.SdkInfo
import com.pusher.platform.logger.AndroidLogger
import com.pusher.platform.logger.LogLevel

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
    override val mediaTypeResolver: MediaTypeResolver by lazy { AndroidMediaTypeResolver() }

}
