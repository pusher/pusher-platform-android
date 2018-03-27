package com.pusher.platform

import com.pusher.SdkInfo
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.ConnectivityHelper

/**
 * Groups dependencies for any SDK created extending this library.
 */
interface PlatformDependencies {
    val logger: Logger
    val scheduler: Scheduler
    val mainScheduler: MainThreadScheduler
    val mediaTypeResolver: MediaTypeResolver
    val connectivityHelper: ConnectivityHelper
    val sdkInfo: SdkInfo
}
