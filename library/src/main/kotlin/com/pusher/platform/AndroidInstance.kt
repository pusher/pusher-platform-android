package com.pusher.platform

import android.content.Context
import com.pusher.platform.logger.AndroidLogger
import com.pusher.platform.logger.LogLevel
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.AndroidConnectivityHelper
import com.pusher.platform.network.ConnectivityHelper

object AndroidInstance {

    @JvmStatic
    @JvmOverloads
    @JvmName("create")
    operator fun invoke(
        locator: String,
        serviceName: String,
        serviceVersion: String,
        context: Context,
        connectivityResolver: ConnectivityHelper = AndroidConnectivityHelper(context),
        baseClient: BaseClient? = null,
        host: String? = null,
        logger: Logger = AndroidLogger(threshold = LogLevel.DEBUG),
        mediaResolver: MediaTypeResolver = AndroidMediaTypeResolver(),
        backgroundScheduler: Scheduler = BackgroundScheduler(),
        foregroundScheduler: MainThreadScheduler = ForegroundScheduler()
    ) = Instance(
        locator,
        serviceName,
        serviceVersion,
        logger,
        backgroundScheduler,
        foregroundScheduler,
        mediaResolver,
        connectivityResolver,
        baseClient,
        host
    )

}
