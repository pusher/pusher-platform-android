package com.pusher.platform

import com.pusher.platform.logger.AndroidLogger
import com.pusher.platform.logger.LogLevel

/**
 * Provides default values for dependencies when using Android.
 */
abstract class AndroidDependencies : PlatformDependencies {

    override val logger by lazy { AndroidLogger(threshold = LogLevel.DEBUG) }
    override val mediaTypeResolver: MediaTypeResolver by lazy { AndroidMediaTypeResolver() }

}
