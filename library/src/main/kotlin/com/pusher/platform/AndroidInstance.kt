package com.pusher.platform

import android.content.Context
import com.pusher.platform.logger.AndroidLogger
import com.pusher.platform.logger.LogLevel
import com.pusher.platform.logger.Logger
import com.pusher.platform.network.AndroidConnectivityHelper
import com.pusher.platform.network.ConnectivityHelper

class AndroidInstance(
    locator: String,
    serviceName: String,
    serviceVersion: String,
    context: Context,
    connectivityResolver: ConnectivityHelper = AndroidConnectivityHelper(context),
    baseClient: BaseClient? = null,
    host: String? = null,
    logger: Logger = AndroidLogger(threshold = LogLevel.DEBUG),
    mediaResolver: MediaTypeResolver = AndroidMediaTypeResolver()
) : Instance(
    locator,
    serviceName,
    serviceVersion,
    logger,
    BackgroundScheduler(serviceName),
    ForegroundScheduler(),
    mediaResolver,
    connectivityResolver,
    baseClient,
    host
)