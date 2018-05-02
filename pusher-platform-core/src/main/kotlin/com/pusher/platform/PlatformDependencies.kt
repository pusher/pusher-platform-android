package com.pusher.platform

import com.pusher.SdkInfo
import com.pusher.platform.logger.Logger

/**
 * Groups dependencies for any SDK created extending this library.
 */
interface PlatformDependencies {
    val logger: Logger
    val mediaTypeResolver: MediaTypeResolver
    val sdkInfo: SdkInfo
}
